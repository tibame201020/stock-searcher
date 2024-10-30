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

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

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
    private final Queue<String> listedStockUrlQueue;

    private final Queue<String> tpexStockUrlQueue;

    public Schedule(StockCrawler stockCrawler, DateProvider dateProvider, TPExStockRepo tpExStockRepo, ListedStockRepo listedStockRepo) {
        this.stockCrawler = stockCrawler;
        this.dateProvider = dateProvider;
        this.tpExStockRepo = tpExStockRepo;
        this.listedStockRepo = listedStockRepo;
        this.listedStockUrlQueue = new LinkedList<>();
        this.tpexStockUrlQueue = new LinkedList<>();
    }

    /**
     * 爬蟲主程式 (30min執行一次)
     * checkImportFile 匯入上市股票資料
     * checkImportTPExFile 匯入上櫃股票資料
     * takeListedStock 取得上市股票資料
     * takeTPExList 取得上櫃股票資料
     */
    @Scheduled(fixedDelay = 1000 * 60 * 60)
    public void updateCompanies() throws Exception {
        new Thread(() -> stockCrawler.getCompanies().subscribe()).start();
    }

    @Scheduled(fixedDelay = 1000 * 60 * 60 * 2)
    public void updateListedCrawlQueue() {
        new Thread(this::takeListedStock).start();
    }

    @Scheduled(fixedDelay = 1000 * 2)
    public void listedCrawlQueueConsumer() {
        if (listedStockUrlQueue.isEmpty()) {
            log.info("取得上市股票資料 queue empty {}", dateProvider.getSystemDateTimeFormat());
            return;
        }
        String url = listedStockUrlQueue.poll();
        log.info("[prepare] 上市股票資料 {}", url);

        List<ListedStock> listedStockList;

        try {
            listedStockList = stockCrawler.fetchListedStockDataFromTWSEApi(url);
        } catch (Exception e) {
            log.error("[error] 取得上市股票資料失敗 {}", e.getMessage());
            listedStockUrlQueue.add(url);
            return;
        }


        int retryCount = 0;
        int maxRetries = 11;

        while (listedStockList.isEmpty() && retryCount < maxRetries) {
            log.info("[empty] fetch 資料為空 {}", url);
            url = listedStockUrlQueue.poll();
            if (null != url) {
                log.info("[next] 上市股票資料 {}", url);
                try {
                    listedStockList = stockCrawler.fetchListedStockDataFromTWSEApi(url);
                } catch (Exception e) {
                    log.error("[error] 取得上市股票資料失敗 {}", e.getMessage());
                    listedStockUrlQueue.add(url);
                    return;
                }
                retryCount++;
            } else {
                log.info("取得上市股票資料 queue empty {}", dateProvider.getSystemDateTimeFormat());
                break;
            }
        }

        listedStockList.forEach(listedStock ->
                log.info("[result] 取得上市股票資料 {}, {}", listedStock.getListedStockId().getCode(), listedStock.getDate()));
        log.info("[remain] 上市股票 Queue: {}", listedStockUrlQueue.size());
        System.gc();
    }

    @Scheduled(fixedDelay = 1000 * 60 * 30)
    public void updateTpexCrawlQueue() {
        takeTPExList();
    }

    @Scheduled(fixedDelay = 1000)
    public void tpexCrawlQueueConsumer() {
        if (tpexStockUrlQueue.isEmpty()) {
            log.info("取得上櫃股票資料 queue empty {}", dateProvider.getSystemDateTimeFormat());
            return;
        }
        String url = tpexStockUrlQueue.poll();
        log.info("[prepare] 上櫃股票資料 {}", url);

        List<TPExStock> tpExStockList;
        try {
            tpExStockList = stockCrawler.fetchTPExStockFromTPEx(url);
        } catch (Exception e) {
            log.error("[error] 取得上櫃股票資料失敗 {}", e.getMessage());
            tpexStockUrlQueue.add(url);
            return;
        }

        int retryCount = 0;
        int maxRetries = 11;

        while (tpExStockList.isEmpty() && retryCount < maxRetries) {
            log.info("[empty] fetch 資料為空 {}", url);
            url = tpexStockUrlQueue.poll();
            if (null != url) {
                log.info("[next] 上櫃股票資料 {}", url);
                try {
                    tpExStockList = stockCrawler.fetchTPExStockFromTPEx(url);
                } catch (Exception e) {
                    log.error("[error] 取得上櫃股票資料失敗 {}", e.getMessage());
                    tpexStockUrlQueue.add(url);
                    return;
                }
                retryCount++;
            } else {
                log.info("取得上櫃股票資料 queue empty {}", dateProvider.getSystemDateTimeFormat());
                break;
            }
        }

        tpExStockList.forEach(result ->
                log.info("[result] 取得上櫃股票資料 {}, {}", result.getTpExStockId().getCode(), result.getTpExStockId().getDate()));
        log.info("[remain] 上櫃股票 Queue: {}", tpexStockUrlQueue.size());
        System.gc();
    }

    /**
     * 上市股票爬蟲
     */
    private void takeListedStock() {
        log.info("[start] 更新需爬蟲上市股票");
        List<CompanyStatus> companyStatusList = stockCrawler.getListedCompanies();
        List<String> previousListedStockUrls = new ArrayList<>(listedStockUrlQueue);

        for (CompanyStatus companyStatus : companyStatusList) {
            if (!companyStatus.isTPE()) {
                String code = companyStatus.getCode();
                List<CodeWithYearMonth> codeWithYearMonthList = new ArrayList<>();

                ListedStock listedStock = listedStockRepo.findFirstByListedStockId_CodeOrderByDateDesc(code)
                        .switchIfEmpty(Mono.defer(() -> {
                            ListedStockId listedStockId = new ListedStockId();
                            listedStockId.setCode(code);
                            ListedStock newListedStock = new ListedStock();
                            newListedStock.setListedStockId(listedStockId);
                            newListedStock.setDate(LocalDate.parse(LISTED_STOCK_CRAWLER_BEGIN));
                            return Mono.just(newListedStock);
                        }))
                        .block();

                if (listedStock != null && (Objects.isNull(listedStock.getUpdateDate()) || filterListedStock(listedStock))) {
                    codeWithYearMonthList.addAll(processCodeWithYearMonthList(listedStock));
                }

                ListedStock earlyListedStock = listedStockRepo.findFirstByListedStockId_CodeOrderByDate(code)
                        .switchIfEmpty(Mono.defer(() -> {
                            ListedStockId listedStockId = new ListedStockId();
                            listedStockId.setCode(code);
                            ListedStock newListedStock = new ListedStock();
                            newListedStock.setListedStockId(listedStockId);
                            newListedStock.setDate(LocalDate.parse(LISTED_STOCK_CRAWLER_BEGIN));
                            return Mono.just(newListedStock);
                        }))
                        .block();

                if (earlyListedStock != null && (Objects.isNull(earlyListedStock.getUpdateDate()) || filterListedStock(earlyListedStock))) {
                    codeWithYearMonthList.addAll(processEarlyCodeWithYearMonthList(earlyListedStock));
                }

                codeWithYearMonthList.stream().distinct()
                        .forEach(codeWithYearMonth -> {
                            String url = getTwseUrl(codeWithYearMonth.getCode(), codeWithYearMonth.getYearMonth());
                            previousListedStockUrls.add(url);
                        });
            }
        }

        List<String> generateListedStockUrls = previousListedStockUrls.stream().distinct().toList();
        listedStockUrlQueue.clear();
        listedStockUrlQueue.addAll(generateListedStockUrls);
        log.info("[end] 更新需爬蟲上市股票");
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
        log.info("[start] 更新需爬蟲上櫃股票");

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

        List<String> previousTpexStockUrls = new ArrayList<>(tpexStockUrlQueue);

        urls.subscribe(
                previousTpexStockUrls::add,
                err -> {},
                () -> {
                    List<String> generateTpexStockUrls = previousTpexStockUrls.stream().distinct().toList();
                    tpexStockUrlQueue.clear();
                    tpexStockUrlQueue.addAll(generateTpexStockUrls);
                    log.info("[end] 更新需爬蟲上櫃股票");
                });
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


    private List<CodeWithYearMonth> processCodeWithYearMonthList(ListedStock listedStock) {
        List<YearMonth> yearMonths = dateProvider.calculateMonthList(listedStock.getDate(), LocalDate.now());
        return yearMonths.stream().map(yearMonth -> {
            CodeWithYearMonth codeWithYearMonth = new CodeWithYearMonth();
            codeWithYearMonth.setCode(listedStock.getListedStockId().getCode());
            codeWithYearMonth.setYearMonth(yearMonth);

            return codeWithYearMonth;
        }).toList();
    }

    private List<CodeWithYearMonth> processEarlyCodeWithYearMonthList(ListedStock listedStock) {
        YearMonth stockYearMonth = YearMonth.of(listedStock.getDate().getYear(), listedStock.getDate().getMonth());
        YearMonth crawlYearMonth = YearMonth.of(LocalDate.parse(LISTED_STOCK_CRAWLER_BEGIN).getYear(), LocalDate.parse(LISTED_STOCK_CRAWLER_BEGIN).getMonth());

        if (stockYearMonth.compareTo(crawlYearMonth) <= 0) {
            return List.of();
        }

        List<YearMonth> yearMonths = dateProvider.calculateMonthList(LocalDate.parse(LISTED_STOCK_CRAWLER_BEGIN), listedStock.getDate());
        return yearMonths.stream().map(yearMonth -> {
            CodeWithYearMonth codeWithYearMonth = new CodeWithYearMonth();
            codeWithYearMonth.setCode(listedStock.getListedStockId().getCode());
            codeWithYearMonth.setYearMonth(yearMonth);

            return codeWithYearMonth;
        }).toList();
    }

}
