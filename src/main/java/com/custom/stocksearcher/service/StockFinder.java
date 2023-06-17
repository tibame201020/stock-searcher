package com.custom.stocksearcher.service;

import com.custom.stocksearcher.models.CompanyStatus;
import com.custom.stocksearcher.models.StockMonthData;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Flux;

import java.util.Map;

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
    Flux<StockMonthData> findStock(String stockCode, String begin, String end);

    /**
     * 根據關鍵字查找company list
     * @param keyword
     * @return
     */
    Flux<CompanyStatus> findCompaniesByKeyWord(String keyword);
}
