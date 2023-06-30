package com.custom.stocksearcher.task;

import com.custom.stocksearcher.models.CodeWithYearMonth;
import com.custom.stocksearcher.models.CompanyStatus;
import com.custom.stocksearcher.models.StockMonthData;
import com.custom.stocksearcher.provider.DateProvider;
import com.custom.stocksearcher.repo.CompanyStatusRepo;
import com.custom.stocksearcher.repo.StockMonthDataRepo;
import com.custom.stocksearcher.service.StockCrawler;
import com.custom.stocksearcher.service.StockFinder;
import jakarta.annotation.PostConstruct;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static com.custom.stocksearcher.constant.Constant.STOCK_CRAWLER_BEGIN;

/**
 * 股價爬蟲Task
 */
@Component
public class Schedule {
    private final Log log = LogFactory.getLog(this.getClass());

    @Autowired
    private StockFinder stockFinder;
    @Autowired
    private StockCrawler stockCrawler;
    @Autowired
    private CompanyStatusRepo companyStatusRepo;
    @Autowired
    private DateProvider dateProvider;
    @Autowired
    private StockMonthDataRepo stockMonthDataRepo;

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
    public void crawlStockData() {
        List<YearMonth> yearMonths = dateProvider.calculateMonthList(
                LocalDate.parse(STOCK_CRAWLER_BEGIN),
                LocalDate.now()
        );
        Flux<CompanyStatus> companyStatusFlux = checkCompaniesData();

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
                        () -> log.info("crawl stockMonthData finish at " + dateProvider.getSystemDateTimeFormat())
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
        return companyStatusFlux.switchIfEmpty(fromOpenApiFlux);
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
