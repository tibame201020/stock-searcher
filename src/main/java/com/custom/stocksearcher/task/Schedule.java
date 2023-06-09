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
import reactor.core.publisher.Mono;

import static com.custom.stocksearcher.constant.Constant.STOCK_CRAWLER_BEGIN;
import static com.custom.stocksearcher.constant.Constant.STOCK_CRAWLER_END;

/**
 * 股價爬蟲Task
 */
@Component
public class Schedule {
    private final Log LOG = LogFactory.getLog(this.getClass());

    @Autowired
    private StockFinder stockFinder;
    @Autowired
    private StockCrawler stockCrawler;
    @Autowired
    private CompanyStatusRepo companyStatusRepo;

    // todo: 取得公司列表 > 取得股價資訊 > 存入elasticsearch > 更新公司 crawler status

    @Scheduled(fixedRate = 1000 * 60)
    public void crawlStockData() {
        Mono<CompanyStatus> companyStatusMono = companyStatusRepo.findFirstByWasCrawler(false);
        companyStatusMono.subscribe(
                companyStatus -> {
                    LOG.info("Received companyStatus: " + companyStatus);
                    crawlerStockDataToLocal(companyStatus.getCode());
                    companyStatus.setWasCrawler(true);
                    companyStatusRepo.save(companyStatus).subscribe();
                },
                error -> {
                    LOG.error("Error occurred: " + error.getMessage());
                },
                () -> {
                    LOG.info("Mono completed");
                }
        );
    }

    /**
     * 確認CompanyStatus是否有公司列表
     * 若無 則從openapi撈取
     * 屬於前置作業
     */
    @PostConstruct
    public void checkCompaniesData() {
        Flux<CompanyStatus> companyStatusFlux = companyStatusRepo.findAll();
        Flux<CompanyStatus> fromOpenApiFlux = Flux.defer(() -> stockCrawler.getCompanies());
        companyStatusFlux.switchIfEmpty(fromOpenApiFlux).subscribe(
                companyStatus -> {
                    LOG.info(String.format("crawl company: %s", companyStatus));
                },
                err -> {
                    LOG.info(String.format("crawl companies: %s", err.getMessage()));
                },
                () -> {
                    LOG.info("crawl company finish");
                }
        );
    }

    /**
     * handle stockFinder findStock method的subscribe
     * @param stockCode 股市代號
     */
    private void crawlerStockDataToLocal(String stockCode) {
        stockFinder.findStock(stockCode, STOCK_CRAWLER_BEGIN, STOCK_CRAWLER_END).subscribe(
                stockData -> {
                    LOG.info(String.format("crawl stock: %s", stockData));
                },
                err -> {
                    LOG.error(String.format("crawl stock: %s error, err: %s", stockCode, err.getMessage()));
                },
                () -> {
                    LOG.info(String.format("crawl stock: %s finish", stockCode));
                }
        );
    }
}
