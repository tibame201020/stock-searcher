package com.custom.stocksearcher.service;

import com.custom.stocksearcher.models.StockData;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Flux;

/**
 * 股價查詢Service
 */
public interface StockFinder {
    Log log = LogFactory.getLog(StockFinder.class);

    /**
     * 查詢股價資訊
     *
     * @param stockCode 股票代號
     * @param begin     開始日期
     * @param end       結束日期
     * @return 股價資訊列表
     */
    Flux<StockData> findStock(String stockCode, String begin, String end);
}
