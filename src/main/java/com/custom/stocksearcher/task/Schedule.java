package com.custom.stocksearcher.task;

import com.custom.stocksearcher.models.CompanyStatus;
import com.custom.stocksearcher.repo.CompanyStatusRepo;
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
import java.util.concurrent.TimeUnit;

import static com.custom.stocksearcher.constant.Constant.STOCK_CRAWLER_BEGIN;
import static com.custom.stocksearcher.constant.Constant.STOCK_CRAWLER_END;

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

    /**
     * 股價爬蟲
     */
    private void crawlStockData() {
        Flux<CompanyStatus> companyStatusFlux = companyStatusRepo.findByWasCrawler(false);
        companyStatusFlux
                .delayElements(Duration.ofSeconds(60))
                .flatMap(companyStatus -> {
                    crawlStockDataToLocal(companyStatus.getCode());
                    companyStatus.setWasCrawler(true);
                    return companyStatusRepo.save(companyStatus);
                })
                .subscribe(companyStatus -> {
                            log.info("receive companyStatus: " + companyStatus);
                        },
                        err -> {
                            log.error("error: " + err.getMessage());
                        },
                        () -> {
                            log.info("companyStatus complete");
                        });

    }

    /**
     * 確認CompanyStatus是否有公司列表
     * 若無 則從openapi撈取
     * 屬於前置作業
     */
    @PostConstruct
    public void checkCompaniesData() {
        Flux<CompanyStatus> companyStatusFlux = companyStatusRepo.findByUpdateDate(LocalDate.now());
        Flux<CompanyStatus> fromOpenApiFlux = Flux.defer(() -> {
            log.info("need update companies list");
            return stockCrawler.getCompanies();
        });
        companyStatusFlux.switchIfEmpty(fromOpenApiFlux).subscribe(
                companyStatus -> {
                    log.info(String.format("crawl company: %s", companyStatus));
                },
                err -> {
                    log.info(String.format("crawl companies: %s", err.getMessage()));
                },
                () -> {
                    log.info("crawl company finish");
                }
        );

        crawlStockData();
    }

    /**
     * handle stockFinder findStock method的subscribe
     *
     * @param stockCode 股市代號
     */
    private void crawlStockDataToLocal(String stockCode) {
        stockFinder.findStock(stockCode, STOCK_CRAWLER_BEGIN, STOCK_CRAWLER_END).subscribe(
                stockData -> {
                    log.info(String.format("crawl stock: %s", stockData));
                },
                err -> {
                    log.error(String.format("crawl stock: %s error, err: %s", stockCode, err.getMessage()));
                },
                () -> {
                    log.info(String.format("crawl stock: %s finish", stockCode));
                }
        );
    }
}
