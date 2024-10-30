package com.custom.stocksearcher.service;

import com.custom.stocksearcher.models.CodeParam;
import com.custom.stocksearcher.models.CompanyStatus;
import com.custom.stocksearcher.models.StockData;
import reactor.core.publisher.Flux;

/**
 * 股價查詢Service
 */
public interface StockFinder {
    /**
     * 查詢股價資訊
     *
     * @param codeParam 查詢bean
     * @return 股價資訊列表
     */
    Flux<StockData> findStockInfo(CodeParam codeParam);

    /**
     * 根據關鍵字查找company
     *
     * @param keyword 關鍵字
     * @return Flux<CompanyStatus>
     */
    Flux<CompanyStatus> findCompaniesByKeyWord(String keyword);

    /**
     * 根據條件查找股價資料(以日K棒數為主)
     *
     * @param codeParam 查詢條件bean
     * @return Flux<StockData>
     */
    Flux<StockData> getStockDataWithKlineCnt(CodeParam codeParam);

}
