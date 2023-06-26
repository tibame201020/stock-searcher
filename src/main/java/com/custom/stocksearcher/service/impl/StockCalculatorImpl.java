package com.custom.stocksearcher.service.impl;

import com.custom.stocksearcher.models.StockBumpy;
import com.custom.stocksearcher.models.StockData;
import com.custom.stocksearcher.service.StockCalculator;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class StockCalculatorImpl implements StockCalculator {

    @Override
    public Mono<StockBumpy> getRangeOfHighAndLowPoint(Flux<StockData> stockDataFlux, String code) {
        return getHighestStockData(stockDataFlux)
                .zipWith(getLowestStockData(stockDataFlux))
                .flatMap(objects -> {
                    StockBumpy stockBumpy = new StockBumpy();
                    stockBumpy.setCode(code);

                    stockBumpy.setHighestDate(objects.getT1().getDate());
                    stockBumpy.setHighestPrice(objects.getT1().getHighestPrice());

                    stockBumpy.setLowestDate(objects.getT2().getDate());
                    stockBumpy.setLowestPrice(objects.getT2().getLowestPrice());

                    stockBumpy.setCalcResult(BigDecimal.ZERO);

                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder
                            .append("\n===============================================\n")
                            .append("股票代號: ").append(stockBumpy.getCode()).append("\n")
                            .append("最高價日期: ").append(stockBumpy.getHighestDate()).append("\n")
                            .append("最高價: ").append(stockBumpy.getHighestPrice()).append("\n")
                            .append("最低價日期: ").append(stockBumpy.getLowestDate()).append("\n")
                            .append("最低價: ").append(stockBumpy.getLowestPrice()).append("\n")
                            .append("計算結果: ").append(stockBumpy.getCalcResult()).append("\n")
                            .append("===============================================");
                    log.info(stringBuilder);

                    return Mono.just(stockBumpy);
                });
    }


    private Mono<StockData> getHighestStockData(Flux<StockData> stockDataFlux) {
        return stockDataFlux.reduce((stockData1, stockData2) ->
                stockData1.getHighestPrice().compareTo(stockData2.getHighestPrice()) > 0 ?
                        stockData1 : stockData2);
    }

    private Mono<StockData> getLowestStockData(Flux<StockData> stockDataFlux) {
        return stockDataFlux.reduce((stockData1, stockData2) ->
                stockData1.getLowestPrice().compareTo(stockData2.getLowestPrice()) < 0 ?
                        stockData1 : stockData2);
    }
}
