package com.custom.stocksearcher.service;

import com.custom.stocksearcher.models.CodeParam;
import com.custom.stocksearcher.models.StockBumpy;
import com.custom.stocksearcher.models.StockData;
import com.custom.stocksearcher.models.StockMAResult;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

/**
 * Stock計算
 */
public interface StockCalculator {
    /**
     * getRangeOfHighAndLowPoint
     *
     * @param stockDataFlux 股票
     * @param codeParam     查詢bean
     * @return result
     */
    Mono<StockBumpy> getRangeOfHighAndLowPoint(Flux<StockData> stockDataFlux, CodeParam codeParam);

    /**
     * 取得StockMAResult Flux
     * 暫定為計算MA5 MA10 MA20 MA60
     *
     * @param stockDataFlux 需計算的StockDataFlux
     * @param code          股票代號
     * @param beginDate     開始日期
     * @param endDate       結束日期
     * @return 計算結果StockMAResult Flux
     */
    Flux<StockMAResult> getStockMa(Flux<StockData> stockDataFlux, String code, LocalDate beginDate, LocalDate endDate);

    /**
     * 預先過濾最後一筆資料
     *
     * @param stockDataFlux 股價資料flux
     * @param codeParam     篩選條件
     * @return 若有通過條件則回傳有有資料
     */
    Flux<StockData> preFilterLastStockData(Flux<StockData> stockDataFlux, CodeParam codeParam);
}
