package com.custom.stocksearcher.service;

import com.custom.stocksearcher.models.StockBumpy;
import com.custom.stocksearcher.models.StockData;
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
     * @return result
     */
    Mono<StockBumpy> getRangeOfHighAndLowPoint(Flux<StockData> stockDataFlux, String code);
}