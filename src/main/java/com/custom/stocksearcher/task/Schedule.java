package com.custom.stocksearcher.task;

import com.custom.stocksearcher.config.LocalDateTypeAdapter;
import com.custom.stocksearcher.models.CodeWithYearMonth;
import com.custom.stocksearcher.models.CompanyStatus;
import com.custom.stocksearcher.models.listed.ListedStock;
import com.custom.stocksearcher.models.listed.ListedStockId;
import com.custom.stocksearcher.models.tpex.TPExStock;
import com.custom.stocksearcher.models.tpex.TPExStockId;
import com.custom.stocksearcher.provider.DateProvider;
import com.custom.stocksearcher.repo.CompanyStatusRepo;
import com.custom.stocksearcher.repo.ListedStockRepo;
import com.custom.stocksearcher.repo.TPExStockRepo;
import com.custom.stocksearcher.service.StockCrawler;
import com.google.gson.GsonBuilder;
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
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

import static com.custom.stocksearcher.constant.Constant.*;

/**
 * 股價爬蟲Task
 */
@Component
public class Schedule {
    private final Log log = LogFactory.getLog(this.getClass());
    @Autowired
    private StockCrawler stockCrawler;
    @Autowired
    private DateProvider dateProvider;
    @Autowired
    private CompanyStatusRepo companyStatusRepo;
    @Autowired
    private TPExStockRepo tpExStockRepo;
    @Autowired
    private ListedStockRepo listedStockRepo;

    /**
     * 爬蟲主程式 (每一小時執行一次)
     * checkImportFile 匯入上市股票資料
     * checkImportTPExFile 匯入上櫃股票資料
     * takeListedStock 取得上市股票資料
     * takeTPExList 取得上櫃股票資料
     */
    @Scheduled(fixedDelay = 1000 * 60 * 60)
    public void crawlStockData() throws Exception {
        checkImportListedFile();
        checkImportTPExFile();
        takeListedStock();
        takeTPExList();
    }

    /**
     * 上市股票爬蟲
     */
    private void takeListedStock() {
        Flux<CompanyStatus> companyStatusFlux = getCompaniesData();

        Flux<String> urls = companyStatusFlux
                .filter(companyStatus -> !companyStatus.isTPE())
                .map(CompanyStatus::getCode)
                .flatMap(code ->
                        listedStockRepo.findFirstByListedStockId_CodeOrderByDateDesc(code)
                                .switchIfEmpty(Mono.defer(() -> {
                                    ListedStockId listedStockId = new ListedStockId();
                                    listedStockId.setCode(code);
                                    ListedStock listedStock = new ListedStock();
                                    listedStock.setListedStockId(listedStockId);
                                    listedStock.setDate(LocalDate.parse(STOCK_CRAWLER_BEGIN));
                                    return Mono.just(listedStock);
                                })))
                .filter(listedStock -> {
                    if (null == listedStock.getUpdateDate()) {
                        return true;
                    } else {
                        return filterListedStock(listedStock);
                    }
                })
                .flatMap(this::processCodeWithYearMonth)
                .sort(Comparator.comparing(CodeWithYearMonth::getCode))
                .map(codeWithYearMonth -> getTwseUrl(codeWithYearMonth.getCode(), codeWithYearMonth.getYearMonth()));

        urls.delayElements(Duration.ofMillis(4500))
                .flatMap(url -> stockCrawler.getListedStockDataFromTWSEApi(url))
                .subscribe(
                        result -> log.info("取得上市股票資料 : " + result.getListedStockId()),
                        err -> log.error(String.format("取得上市股票資料錯誤: %s", err)),
                        () -> {
                            log.info("上市股票資料更新完畢: " + dateProvider.getSystemDateTimeFormat());
//                            writeListedToFile();
                        }
                );


    }

    /**
     * 過濾需要爬蟲的listedStock
     *
     * @param listedStock listedStock
     * @return boolean
     */
    private boolean filterListedStock(ListedStock listedStock) {
        LocalDate sysDate = LocalDate.now();
        LocalDate date = listedStock.getDate();
        LocalDate updateDate = listedStock.getUpdateDate();
        int hour = LocalDateTime.now().getHour();

        if (sysDate.isEqual(date)) {
            return false;
        }

        if (sysDate.isEqual(updateDate) && (hour < 15)) {
            YearMonth sysYearMonth = YearMonth.from(sysDate);
            YearMonth stockYearMonth = YearMonth.from(date);
            return !stockYearMonth.atEndOfMonth().isEqual(sysYearMonth.atEndOfMonth());
        }

        return true;

    }

    /**
     * 上櫃股票爬蟲
     */
    private void takeTPExList() {
        Mono<TPExStock> defaultTpExStockMono = Mono.defer(() -> {
            TPExStockId tpExStockId = new TPExStockId();
            tpExStockId.setDate(LocalDate.parse(STOCK_CRAWLER_BEGIN));
            TPExStock tpExStock = new TPExStock();
            tpExStock.setTpExStockId(tpExStockId);
            return Mono.just(tpExStock);
        });

        Mono<TPExStock> tpExStockMono = tpExStockRepo.findFirstByOrderByDateDescUpdateDateDesc()
                .switchIfEmpty(defaultTpExStockMono);

        Flux<String> urls = Flux.from(tpExStockMono)
                .flatMap(tpExStock -> Mono.just(tpExStock.getTpExStockId().getDate().minusDays(1)))
                .flatMap(beginDate -> Flux.fromIterable(dateProvider.calculateMonthList(beginDate, LocalDate.now())))
                .flatMap(yearMonth ->
                        Flux.range(1, yearMonth.lengthOfMonth()).map(yearMonth::atDay))
                .filter(date -> date.isBefore(LocalDate.now().plusDays(1)))
                .sort()
                .flatMap(date -> Mono.just(date.toString().replaceAll("-", "/")))
                .flatMap(dateStr -> Mono.just(String.format(TPEx_LIST_URL, dateStr)));

        urls.delayElements(Duration.ofMillis(1000))
                .flatMap(url -> stockCrawler.getTPExStockFromTPEx(url))
                .subscribe(
                        result -> log.info("取得上櫃股票資料 : " + result.getTpExStockId()),
                        err -> log.error(String.format("取得上櫃股票資料錯誤: %s", err)),
                        () -> {
                            log.info("上櫃股票資料更新完畢: " + dateProvider.getSystemDateTimeFormat());
//                            writeTPEXToFile();
                        }
                );
    }

    /**
     * 上櫃股價資料匯入
     */
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
                .buffer()
                .flatMap(tpExStockList -> tpExStockRepo.saveAll(tpExStockList))
                .subscribe(
                        tpExStock -> log.info("save to elasticsearch : " + tpExStock),
                        err -> log.error("error : " + err.getMessage()),
                        () -> log.info("import tpExStock finish at " + dateProvider.getSystemDateTimeFormat())
                );
    }

    /**
     * 上市股價資料匯入
     */
    private void checkImportListedFile() throws IOException {
        String file = "stocksListed";
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
                                .create().fromJson(str, ListedStock.class)))
                .buffer()
                .flatMap(listedStockList -> listedStockRepo.saveAll(listedStockList))
                .subscribe(
                        listedStock -> log.info("save to elasticsearch : " + listedStock),
                        err -> log.error("error : " + err.getMessage()),
                        () -> log.info("import listedStock finish at " + dateProvider.getSystemDateTimeFormat())
                );
    }

    /**
     * 上櫃股價資料匯出
     */
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

    /**
     * 上市股價資料匯出
     */
    private void writeListedToFile() {
        String file = "stocksListed";
        Flux<ListedStock> stockMonthDataFlux = listedStockRepo.findAll();
        Flux<String> dataFlux = stockMonthDataFlux
                .flatMap(listedStock -> Flux.just(
                        new GsonBuilder().registerTypeAdapter(LocalDate.class, new LocalDateTypeAdapter())
                                .create()
                                .toJson(listedStock)));
        Path path = Paths.get(file);
        writeFile(dataFlux, path).subscribe();
    }

    /**
     * 將data寫至path
     *
     * @param dataFlux 資料
     * @param path     目標檔案
     * @return Flux<String>
     */
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
    public Flux<CompanyStatus> getCompaniesData() {
        return Flux.defer(() -> {
            log.info("update companies list");
            return stockCrawler.getCompanies();
        });
    }


    /**
     * 取得twse url
     *
     * @param code      股票代號
     * @param yearMonth 月份
     * @return url
     */
    private String getTwseUrl(String code, YearMonth yearMonth) {
        String date = yearMonth.atDay(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return String.format(STOCK_INFO_URL, date, code);
    }

    /**
     * 包裝準備爬取資料
     *
     * @param listedStock db中最後一筆
     * @return 剩餘準備爬取資料
     */
    private Flux<CodeWithYearMonth> processCodeWithYearMonth(ListedStock listedStock) {
        List<YearMonth> yearMonths = dateProvider.calculateMonthList(listedStock.getDate(), LocalDate.now());
        return Flux.fromIterable(yearMonths).map(yearMonth -> {
            CodeWithYearMonth codeWithYearMonth = new CodeWithYearMonth();
            codeWithYearMonth.setCode(listedStock.getListedStockId().getCode());
            codeWithYearMonth.setYearMonth(yearMonth);

            return codeWithYearMonth;
        });
    }

}
