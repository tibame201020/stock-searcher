package com.custom.stocksearcher.task;

import com.custom.stocksearcher.models.CodeWithYearMonth;
import com.custom.stocksearcher.models.CompanyStatus;
import com.custom.stocksearcher.models.listed.ListedStock;
import com.custom.stocksearcher.models.listed.ListedStockId;
import com.custom.stocksearcher.models.tpex.TPExStock;
import com.custom.stocksearcher.models.tpex.TPExStockId;
import com.custom.stocksearcher.provider.DateProvider;
import com.custom.stocksearcher.repo.ListedStockRepo;
import com.custom.stocksearcher.repo.TPExStockRepo;
import com.custom.stocksearcher.service.StockCrawler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import static com.custom.stocksearcher.constant.Constant.*;

/**
 * 股價爬蟲Task
 */
@Component
@Slf4j
public class Schedule {
    private final StockCrawler stockCrawler;
    private final DateProvider dateProvider;
    private final TPExStockRepo tpExStockRepo;
    private final ListedStockRepo listedStockRepo;

    public Schedule(StockCrawler stockCrawler, DateProvider dateProvider, TPExStockRepo tpExStockRepo, ListedStockRepo listedStockRepo) {
        this.stockCrawler = stockCrawler;
        this.dateProvider = dateProvider;
        this.tpExStockRepo = tpExStockRepo;
        this.listedStockRepo = listedStockRepo;
    }

    /**
     * 爬蟲主程式 (30min執行一次)
     * checkImportFile 匯入上市股票資料
     * checkImportTPExFile 匯入上櫃股票資料
     * takeListedStock 取得上市股票資料
     * takeTPExList 取得上櫃股票資料
     */
    @Scheduled(fixedDelay = 1000 * 60 * 30)
    public void crawlStockData() throws Exception {
        takeListedStock();
        takeTPExList();
    }

    /**
     * 上市股票爬蟲
     */
    private void takeListedStock() {
        Flux<CompanyStatus> companyStatusFlux = getCompaniesData().log("[ company ]");

        Flux<String> urls = companyStatusFlux
                .parallel()
                .runOn(Schedulers.parallel())
                .filter(companyStatus -> !companyStatus.isTPE())
                .map(CompanyStatus::getCode)
                .flatMap(code ->
                        listedStockRepo.findFirstByListedStockId_CodeOrderByDateDesc(code)
                                .switchIfEmpty(Mono.defer(() -> {
                                    ListedStockId listedStockId = new ListedStockId();
                                    listedStockId.setCode(code);
                                    ListedStock listedStock = new ListedStock();
                                    listedStock.setListedStockId(listedStockId);
                                    listedStock.setDate(LocalDate.parse(LISTED_STOCK_CRAWLER_BEGIN));
                                    return Mono.just(listedStock);
                                })))
                .filter(listedStock -> {
                    if (Objects.isNull(listedStock.getUpdateDate())) {
                        return true;
                    } else {
                        return filterListedStock(listedStock);
                    }
                })
                .flatMap(this::processCodeWithYearMonth)
                .log()
                .sequential()
                .sort(Comparator.comparing(CodeWithYearMonth::getCode))
                .map(codeWithYearMonth -> getTwseUrl(codeWithYearMonth.getCode(), codeWithYearMonth.getYearMonth()))
                .distinct();

        urls.delayElements(Duration.ofMillis(LISTED_CRAWL_DURATION_MILLS))
                .flatMap(stockCrawler::getListedStockDataFromTWSEApi)
                .subscribe(
                        result -> log.info("取得上市股票資料 : {}, {}", result.getListedStockId().getCode(), result.getListedStockId().getDate()),
                        err -> log.error("取得上市股票資料錯誤: " + err),
                        () -> log.info("上市股票資料更新完畢: " + dateProvider.getSystemDateTimeFormat())
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
        LocalDate stockDate = listedStock.getDate();
        LocalDate updateDate = listedStock.getUpdateDate();
        int hour = LocalDateTime.now().getHour();

        YearMonth sysYearMonth = YearMonth.from(sysDate);
        YearMonth stockYearMonth = YearMonth.from(stockDate);

        if (sysDate.isEqual(stockDate)) {
            return false;
        }

        if (sysDate.minusDays(1).isEqual(updateDate)
                && sysDate.minusDays(1).isEqual(stockDate)
                && (hour <= LISTED_CRAWL_UPDATE_HOUR)) {
            return !stockYearMonth.atEndOfMonth().isEqual(sysYearMonth.atEndOfMonth());
        }

        if (sysDate.isEqual(updateDate) && (hour <= LISTED_CRAWL_UPDATE_HOUR)) {
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
            tpExStockId.setDate(LocalDate.parse(TPEx_STOCK_CRAWLER_BEGIN));
            TPExStock tpExStock = new TPExStock();
            tpExStock.setTpExStockId(tpExStockId);
            return Mono.just(tpExStock);
        });

        Mono<TPExStock> tpExStockMono = tpExStockRepo.findFirstByOrderByDateDesc()
                .switchIfEmpty(defaultTpExStockMono);

        Flux<String> urls = Flux.from(tpExStockMono)
                .filter(tpExStock -> {
                    if (Objects.isNull(tpExStock.getUpdateDate())) {
                        return true;
                    } else {
                        return filterTPExStock(tpExStock);
                    }
                })
                .map(tpExStock -> tpExStock.getTpExStockId().getDate().minusDays(1))
                .flatMap(beginDate -> Flux.fromIterable(dateProvider.calculateMonthList(beginDate, LocalDate.now())))
                .flatMap(yearMonth ->
                        Flux.range(1, yearMonth.lengthOfMonth()).map(yearMonth::atDay))
                .filter(date -> date.isBefore(LocalDate.now().plusDays(1)))
                .sort()
                .map(date -> dateProvider.localDateToString(date, STOCK_DATE_FORMAT))
                .map(dateStr -> String.format(TPEx_LIST_URL, dateStr));

        urls.delayElements(Duration.ofMillis(TPEX_CRAWL_DURATION_MILLS))
                .flatMap(stockCrawler::getTPExStockFromTPEx)
                .subscribe(
                        result -> log.info("取得上櫃股票資料 : {}, {}", result.getTpExStockId().getCode(), result.getTpExStockId().getDate()),
                        err -> log.error("取得上櫃股票資料錯誤: " + err),
                        () -> log.info("上櫃股票資料更新完畢: " + dateProvider.getSystemDateTimeFormat())
                );
    }

    private boolean filterTPExStock(TPExStock tpExStock) {
        LocalDate sysDate = LocalDate.now();
        LocalDate stockDate = tpExStock.getDate();
        LocalDate updateDate = tpExStock.getUpdateDate();
        int hour = LocalDateTime.now().getHour();

        if (sysDate.isEqual(stockDate)) {
            return false;
        }

        return !sysDate.isEqual(updateDate) || (hour > LISTED_CRAWL_UPDATE_HOUR);
    }


    /**
     * 確認CompanyStatus是否有公司列表
     * 若無 則從openapi撈取
     * 屬於前置作業
     */
    public Flux<CompanyStatus> getCompaniesData() {
        return Flux.defer(stockCrawler::getCompanies);
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
