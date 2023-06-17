package com.custom.stocksearcher.service;

import com.custom.stocksearcher.models.CompanyStatus;
import com.custom.stocksearcher.models.StockMonthData;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 股價爬蟲Service
 */
public interface StockCrawler {

    Log log = LogFactory.getLog(StockCrawler.class);

    /**
     * 從twse取得股票資訊
     *
     * @param stockCode 股票代碼
     * @param dateStr   日期
     * @return Flux<StockData>
     */
    Mono<StockMonthData> getStockMonthDataFromTWSEApi(String stockCode, String dateStr);

    /**
     * 從twse open api 取得公司名稱與股票代號
     *
     * @return 公司名稱與股票代號列表
     */
    Flux<CompanyStatus> getCompanies();
}
