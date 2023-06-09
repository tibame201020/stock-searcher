package com.custom.stocksearcher.service;

import com.custom.stocksearcher.models.CompanyStatus;
import com.custom.stocksearcher.models.StockData;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Flux;

/**
 * 股價爬蟲Service
 */
public interface StockCrawler {

    Log LOG = LogFactory.getLog(StockCrawler.class);

    /**
     * 從twse取得股票資訊
     * @param stockCode 股票代碼
     * @param dateStr 日期
     * @return Flux<StockData>
     */
    Flux<StockData> getStockDataLs(String stockCode, String dateStr);

    /**
     * 從twse open api 取得公司名稱與股票代號
     * @return 公司名稱與股票代號列表
     */
    Flux<CompanyStatus> getCompanies();
}
