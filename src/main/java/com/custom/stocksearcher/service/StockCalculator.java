package com.custom.stocksearcher.service;

import com.custom.stocksearcher.models.CodeParam;
import com.custom.stocksearcher.models.StockBumpy;
import com.custom.stocksearcher.models.StockData;
import com.custom.stocksearcher.models.StockMAResult;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Stock計算
 */
public interface StockCalculator {
    Log log = LogFactory.getLog(StockCalculator.class);

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
     * @return 計算結果StockMAResult Flux
     */
    Flux<StockMAResult> getStockMa(Flux<StockData> stockDataFlux, String code);

    Flux<StockData> preFilterLastStockData(Flux<StockData> stockDataFlux, CodeParam codeParam);
}
