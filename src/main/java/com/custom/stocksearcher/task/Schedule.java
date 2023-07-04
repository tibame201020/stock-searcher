package com.custom.stocksearcher.task;

import com.custom.stocksearcher.config.LocalDateTypeAdapter;
import com.custom.stocksearcher.models.CodeWithYearMonth;
import com.custom.stocksearcher.models.CompanyStatus;
import com.custom.stocksearcher.models.StockMonthData;
import com.custom.stocksearcher.models.tpex.TPExStock;
import com.custom.stocksearcher.models.tpex.TPExStockId;
import com.custom.stocksearcher.provider.DateProvider;
import com.custom.stocksearcher.repo.CompanyStatusRepo;
import com.custom.stocksearcher.repo.StockMonthDataRepo;
import com.custom.stocksearcher.repo.TPExStockRepo;
import com.custom.stocksearcher.service.StockCrawler;
import com.google.gson.GsonBuilder;
import jakarta.annotation.PostConstruct;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

import static com.custom.stocksearcher.constant.Constant.STOCK_CRAWLER_BEGIN;
import static com.custom.stocksearcher.constant.Constant.TPEx_LIST_URL;

/**
 * 股價爬蟲Task
 */
@Component
public class Schedule {
    private final Log log = LogFactory.getLog(this.getClass());
    @Autowired
    private StockCrawler stockCrawler;
    @Autowired
    private CompanyStatusRepo companyStatusRepo;
    @Autowired
    private DateProvider dateProvider;
    @Autowired
    private StockMonthDataRepo stockMonthDataRepo;
    @Autowired
    private TPExStockRepo tpExStockRepo;

    /**
     * 爬蟲主程式 (啟動時執行一次 每日九點執行一次)
     * 1.yearMonths: 抓取資料日期區間
     * 2.companyStatusFlux: 全上市公司列表，每日更新一次
     * 3.codeYearMonthFlux: 股市代號與日期combine
     * 4.emptyStockMonthDataFlux: 先用codeYearMonthFlux去local撈資料 若無則透過switchIfEmpty與filter做出無資料的Flux
     * 5.最終使用emptyStockMonthDataFlux開始爬資料
     */
    @Scheduled(cron = "0 0 2 * * *")
    @PostConstruct
    public void crawlStockData() throws Exception {
        checkImportFile();
        checkImportTPExFile();
        takeListedStock();
        takeTPExList();
    }

    private void takeListedStock() {
        Flux<CompanyStatus> companyStatusFlux = checkCompaniesData();
        List<YearMonth> yearMonths = dateProvider.calculateMonthList(
                LocalDate.parse(STOCK_CRAWLER_BEGIN),
                LocalDate.now()
        );

        Flux<CodeWithYearMonth> codeYearMonthFlux = companyStatusFlux.flatMap(companyStatus ->
                Flux.fromIterable(yearMonths).map(yearMonth -> new CodeWithYearMonth(companyStatus.getCode(), yearMonth))
        );

        Flux<StockMonthData> emptyStockMonthDataFlux = codeYearMonthFlux
                .flatMap(codeWithYearMonth -> {
                    String code = codeWithYearMonth.getCode();
                    YearMonth yearMonth = codeWithYearMonth.getYearMonth();
                    Flux<StockMonthData> stockMonthDataFlux = getStockMonthDataFluxFromDB(code, yearMonth);

                    StockMonthData stockMonthData = new StockMonthData();
                    stockMonthData.setCode(code);
                    stockMonthData.setYearMonth(yearMonth.toString());

                    return Flux.from(stockMonthDataFlux).switchIfEmpty(Flux.defer(() -> Flux.just(stockMonthData)));
                })
                .filter(stockMonthData -> null == stockMonthData.getStockMonthDataId());

        log.info("start crawl stockMonthData at " + dateProvider.getSystemDateTimeFormat());
        emptyStockMonthDataFlux
                .delayElements(Duration.ofSeconds(6))
                .flatMap(stockMonthData ->
                        getStockMonthDataFluxFromOpenApi(stockMonthData.getCode(), YearMonth.parse(stockMonthData.getYearMonth()))
                )
                .subscribe(
                        result -> {
                        },
                        err -> {
                            err.printStackTrace();
                            log.error(String.format("get stockMonthData error: %s", err));
                        },
                        () -> {
                            log.info("crawl stockMonthData finish at " + dateProvider.getSystemDateTimeFormat());
//                            writeToFile();
                        }
                );
    }

    private void takeTPExList() {
        Flux<TPExStock> defaultTpExStockMono = Flux.defer(() -> {
            TPExStockId tpExStockId = new TPExStockId();
            tpExStockId.setDate(LocalDate.parse(STOCK_CRAWLER_BEGIN));
            TPExStock tpExStock = new TPExStock();
            tpExStock.setTpExStockId(tpExStockId);
            return Mono.just(tpExStock);
        });

        Mono<TPExStock> tpExStockMono = tpExStockRepo.findAll()
                .sort(Comparator.comparing(TPExStock::getUpdateDate))
                .sort(Comparator.comparing(tpExStock -> tpExStock.getTpExStockId().getDate()))
                .switchIfEmpty(defaultTpExStockMono)
                .last();

        tpExStockMono
                .flux()
                .flatMap(tpExStock -> Mono.just(tpExStock.getTpExStockId().getDate().minusDays(7)))
                .flatMap(beginDate -> Flux.fromIterable(dateProvider.calculateMonthList(beginDate, LocalDate.now())))
                .flatMap(yearMonth ->
                        Flux.range(1, yearMonth.lengthOfMonth()).map(yearMonth::atDay))
                .filter(date -> date.isBefore(LocalDate.now()))
                .flatMap(date -> Mono.just(date.toString().replaceAll("-", "/")))
                .flatMap(dateStr -> Mono.just(String.format(TPEx_LIST_URL, dateStr)))
                .flatMap(url -> stockCrawler.getTPExStockFromTPEx(url))
                .subscribe(
                        result -> {
                            log.info("get TPExStock : " + result);
                        },
                        err -> {
                            err.printStackTrace();
                            log.error(String.format("get TPExStock error: %s", err));
                        },
                        () -> {
                            log.info("crawl TPExStock finish at " + dateProvider.getSystemDateTimeFormat());
//                            writeTPEXToFile();
                        }
                );
    }

    private void checkImportTPExFile() throws IOException {
        String file = "stocksTPEX";
        Path path = Paths.get(file);
        boolean exists = Files.exists(path);
        if (!exists) {
            return;
        }

        String json = Files.readString(path);
        String[] strArray = json.split("\n");

        Flux.fromArray(strArray)
                .flatMap(
                        str -> Mono.just(new GsonBuilder().registerTypeAdapter(LocalDate.class, new LocalDateTypeAdapter())
                                .create().fromJson(str, TPExStock.class)))
                .flatMap(tpExStock -> tpExStockRepo.save(tpExStock))
                .subscribe(
                        tpExStock -> log.info("save to elasticsearch : " + tpExStock),
                        err -> log.error("error : " + err.getMessage()),
                        () -> log.info("import tpExStock finish at " + dateProvider.getSystemDateTimeFormat())
                );
    }

    private void checkImportFile() throws IOException {
        String file = "stocks";
        Path path = Paths.get(file);
        boolean exists = Files.exists(path);
        if (!exists) {
            return;
        }

        String json = Files.readString(path);
        String[] strArray = json.split("\n");

        Flux.fromArray(strArray)
                .flatMap(
                        str -> Mono.just(new GsonBuilder().registerTypeAdapter(LocalDate.class, new LocalDateTypeAdapter())
                                .create().fromJson(str, StockMonthData.class)))
                .flatMap(stockMonthData -> stockMonthDataRepo.save(stockMonthData))
                .subscribe(
                        stockMonthData -> log.info("save to elasticsearch : " + stockMonthData),
                        err -> log.error("error : " + err.getMessage()),
                        () -> log.info("import stockMonthData finish at " + dateProvider.getSystemDateTimeFormat())
                );
    }

    private void writeTPEXToFile() {
        String file = "stocksTPEX";
        Flux<TPExStock> tpExStockRepoAll = tpExStockRepo.findAll();
        Flux<String> dataFlux = tpExStockRepoAll
                .flatMap(tpExStock -> Flux.just(
                        new GsonBuilder().registerTypeAdapter(LocalDate.class, new LocalDateTypeAdapter())
                                .create()
                                .toJson(tpExStock)));
        Path path = Paths.get(file);
        writeFile(dataFlux, path).subscribe();
    }


    private void writeToFile() {
        String file = "stocks";
        Flux<StockMonthData> stockMonthDataFlux = stockMonthDataRepo.findAll();
        Flux<String> dataFlux = stockMonthDataFlux
                .flatMap(stockMonthData -> Flux.just(
                        new GsonBuilder().registerTypeAdapter(LocalDate.class, new LocalDateTypeAdapter())
                                .create()
                                .toJson(stockMonthData)));
        Path path = Paths.get(file);
        writeFile(dataFlux, path).subscribe();
    }

    private Flux<String> writeFile(Flux<String> dataFlux, Path path) {
        return Flux.using(
                () -> Files.newBufferedWriter(path, StandardOpenOption.CREATE),
                writer -> dataFlux.doOnNext(line -> {
                    try {
                        writer.write(line + System.lineSeparator());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }),
                writer -> {
                    try {
                        writer.flush();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    try {
                        writer.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
        );
    }

    /**
     * 確認CompanyStatus是否有公司列表
     * 若無 則從openapi撈取
     * 屬於前置作業
     */
    public Flux<CompanyStatus> checkCompaniesData() {
        Flux<CompanyStatus> companyStatusFlux = companyStatusRepo.findByUpdateDate(LocalDate.now());
        Flux<CompanyStatus> fromOpenApiFlux = Flux.defer(() -> {
            log.info("need update companies list");
            return stockCrawler.getCompanies();
        });
        return companyStatusFlux.switchIfEmpty(fromOpenApiFlux).filter(companyStatus -> !companyStatus.isTPE());
    }

    private Flux<StockMonthData> getStockMonthDataFluxFromDB(String code, YearMonth yearMonth) {
        YearMonth currentMonth = YearMonth.now();
        Flux<StockMonthData> stockMonthDataFlux;
        if (yearMonth.equals(currentMonth)) {
            stockMonthDataFlux = stockMonthDataRepo.findByCodeAndYearMonthAndIsHistoryAndUpdateDate(code, yearMonth.toString(), false, LocalDate.now());
        } else {
            stockMonthDataFlux = stockMonthDataRepo.findByCodeAndYearMonthAndIsHistory(code, yearMonth.toString(), true);
        }

        return stockMonthDataFlux;
    }


    private Flux<StockMonthData> getStockMonthDataFluxFromOpenApi(String code, YearMonth yearMonth) {
        return stockCrawler.getStockMonthDataFromTWSEApi(code, yearMonth.atDay(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"))).flux();
    }


}
