package com.custom.stocksearcher.service.impl;

import com.custom.stocksearcher.models.StockBumpy;
import com.custom.stocksearcher.models.StockData;
import com.custom.stocksearcher.models.StockMAResult;
import com.custom.stocksearcher.models.StockMAResultId;
import com.custom.stocksearcher.repo.StockMAResultRepo;
import com.custom.stocksearcher.service.StockCalculator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class StockCalculatorImpl implements StockCalculator {

    @Autowired
    private StockMAResultRepo stockMAResultRepo;

    @Override
    public Mono<StockBumpy> getRangeOfHighAndLowPoint(Flux<StockData> stockDataFlux, String code) {
        return getHighestStockData(stockDataFlux)
                .zipWith(getLowestStockData(stockDataFlux))
                .zipWith(getLowTradeVolume(stockDataFlux))
                .map(tuple2 -> Tuples.of(tuple2.getT1().getT1(), tuple2.getT1().getT2(), tuple2.getT2()))
                .flatMap(objects -> {
                    StockBumpy stockBumpy = new StockBumpy();
                    stockBumpy.setCode(code);

                    stockBumpy.setHighestDate(objects.getT1().getDate());
                    stockBumpy.setHighestPrice(objects.getT1().getHighestPrice());

                    stockBumpy.setLowestDate(objects.getT2().getDate());
                    stockBumpy.setLowestPrice(objects.getT2().getLowestPrice());

                    stockBumpy.setLowestTradeVolumeDate(objects.getT3().getDate());
                    stockBumpy.setLowestTradeVolume(objects.getT3().getTradeVolume());

                    BigDecimal calcResult = BigDecimal.ZERO;

                    stockBumpy.setCalcResult(calcResult);

                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder
                            .append("\n===============================================\n")
                            .append("股票代號: ").append(stockBumpy.getCode()).append("\n")
                            .append("最高價日期: ").append(stockBumpy.getHighestDate()).append("\n")
                            .append("最高價: ").append(stockBumpy.getHighestPrice()).append("\n")
                            .append("最低價日期: ").append(stockBumpy.getLowestDate()).append("\n")
                            .append("最低價: ").append(stockBumpy.getLowestPrice()).append("\n")
                            .append("最低成交量日期: ").append(stockBumpy.getLowestTradeVolume()).append("\n")
                            .append("最低成交量: ").append(stockBumpy.getLowestTradeVolume()).append("\n")
                            .append("計算結果: ").append(stockBumpy.getCalcResult()).append("\n")
                            .append("===============================================");
                    log.info(stringBuilder);

                    return Mono.just(stockBumpy);
                });
    }

    @Override
    public Flux<StockMAResult> getStockMa(Flux<StockData> stockDataFlux, String code) {
        return stockDataFlux
                .buffer(60, 1)
                .filter(list -> list.size() >= 60)
                .flatMap(window -> {
                    StockMAResultId stockMAResultId = new StockMAResultId();
                    stockMAResultId.setCode(code);
                    stockMAResultId.setDate(window.get(window.size() - 1).getDate());

                    Mono<StockMAResult> stockMAResultMono = stockMAResultRepo.findById(stockMAResultId);
                    Mono<StockMAResult> calcStockMaResult = Mono.defer(() -> stockMAResultRepo.save(calcStockMa(window, code)));

                    return stockMAResultMono.switchIfEmpty(calcStockMaResult);
                });
    }

    private Mono<StockData> getLowTradeVolume(Flux<StockData> stockDataFlux) {
        return stockDataFlux.reduce((stockData1, stockData2) ->
                stockData1.getTradeVolume().compareTo(stockData2.getTradeVolume()) < 0 ?
                        stockData1 : stockData2);
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

    private StockMAResult calcStockMa(List<StockData> window, String code) {
        BigDecimal ma5 = calculateMA(window, 5);
        BigDecimal ma10 = calculateMA(window, 10);
        BigDecimal ma20 = calculateMA(window, 20);
        BigDecimal ma60 = calculateMA(window, 60);
        StockData lastData = window.get(window.size() - 1);

        StockMAResult stockMAResult = new StockMAResult();
        stockMAResult.setMa5(ma5);
        stockMAResult.setMa10(ma10);
        stockMAResult.setMa20(ma20);
        stockMAResult.setMa60(ma60);
        stockMAResult.setCode(code);
        stockMAResult.setDate(lastData.getDate());

        StockMAResultId stockMAResultId = new StockMAResultId();
        stockMAResultId.setCode(code);
        stockMAResultId.setDate(lastData.getDate());

        stockMAResult.setStockMAResultId(stockMAResultId);

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("\n").append("==============================").append("\n");
        stringBuilder.append("計算MA").append("\n");
        stringBuilder.append("代號: ").append(code).append("\n");
        stringBuilder.append("日期: ").append(lastData.getDate()).append("\n");
        stringBuilder.append("stockMAResult: ").append(stockMAResult).append("\n");
        stringBuilder.append("==============================").append("\n");
        log.info(stringBuilder);

        return stockMAResult;
    }

    private BigDecimal calculateMA(List<StockData> window, int period) {
        if (window.size() < period) {
            return null;
        }

        BigDecimal sum = BigDecimal.ZERO;
        for (int i = window.size() - period; i < window.size(); i++) {
            BigDecimal closingPrice = window.get(i).getClosingPrice();
            if (null == closingPrice) {
                closingPrice = BigDecimal.ZERO;
            }
            sum = sum.add(closingPrice);
        }

        return sum.divide(BigDecimal.valueOf(period), RoundingMode.HALF_UP);
    }


}
