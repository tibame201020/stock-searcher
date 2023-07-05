package com.custom.stocksearcher.service;

import com.custom.stocksearcher.models.CodeParam;
import com.custom.stocksearcher.models.CompanyStatus;
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
     * @param codeParam 查詢bean
     * @return 股價資訊列表
     */
    Flux<StockData> findStockInfo(CodeParam codeParam);

    /**
     * 根據關鍵字查找company list
     *
     * @param keyword
     * @return
     */
    Flux<CompanyStatus> findCompaniesByKeyWord(String keyword);

}
