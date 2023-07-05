package com.custom.stocksearcher.service.impl;

import com.custom.stocksearcher.models.*;
import com.custom.stocksearcher.repo.CompanyStatusRepo;
import com.custom.stocksearcher.repo.StockMAResultRepo;
import com.custom.stocksearcher.service.StockCalculator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Service
public class StockCalculatorImpl implements StockCalculator {

    @Autowired
    private StockMAResultRepo stockMAResultRepo;

    @Autowired
    private CompanyStatusRepo companyStatusRepo;

    @Override
    public Mono<StockBumpy> getRangeOfHighAndLowPoint(Flux<StockData> stockDataFlux, CodeParam codeParam) {

        return getHighestStockData(stockDataFlux)
                .zipWith(getLowestStockData(stockDataFlux))
                .zipWith(getLowTradeVolume(stockDataFlux))
                .map(tuple2 -> Tuples.of(tuple2.getT1().getT1(), tuple2.getT1().getT2(), tuple2.getT2()))
                .flatMap(objects -> {
                    StockBumpy stockBumpy = new StockBumpy();
                    stockBumpy.setCode(codeParam.getCode());

                    stockBumpy.setHighestDate(objects.getT1().getDate());
                    stockBumpy.setHighestPrice(objects.getT1().getHighestPrice());

                    stockBumpy.setLowestDate(objects.getT2().getDate());
                    stockBumpy.setLowestPrice(objects.getT2().getLowestPrice());

                    stockBumpy.setLowestTradeVolumeDate(objects.getT3().getDate());
                    stockBumpy.setLowestTradeVolume(objects.getT3().getTradeVolume());

                    BigDecimal calcResult = stockBumpy.getHighestPrice()
                            .subtract(stockBumpy.getLowestPrice())
                            .divide(stockBumpy.getLowestPrice(), 4, RoundingMode.FLOOR)
                            .multiply(BigDecimal.valueOf(100));

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
                }).flatMap(stockBumpy ->
                        companyStatusRepo
                                .findById(stockBumpy.getCode())
                                .flatMap(companyStatus -> {
                                    stockBumpy.setName(companyStatus.getName());
                                    return Mono.just(stockBumpy);
                                })
                );
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

    /**
     * 取得區間最少交易量
     *
     * @param stockDataFlux 股價Flux
     * @return Mono<StockData>
     */
    private Mono<StockData> getLowTradeVolume(Flux<StockData> stockDataFlux) {
        return stockDataFlux.reduce((stockData1, stockData2) ->
                stockData1.getTradeVolume().compareTo(stockData2.getTradeVolume()) < 0 ?
                        stockData1 : stockData2);
    }

    /**
     * 取得區間最高價
     *
     * @param stockDataFlux 股價Flux
     * @return Mono<StockData>
     */
    private Mono<StockData> getHighestStockData(Flux<StockData> stockDataFlux) {
        return stockDataFlux.reduce((stockData1, stockData2) -> {
            BigDecimal stock1Price = getStockDataHighest(stockData1);
            BigDecimal stock2Price = getStockDataHighest(stockData2);
            stockData1.setHighestPrice(stock1Price);
            stockData2.setHighestPrice(stock2Price);

            if (stock1Price == null) {
                return stockData2;
            }
            if (stock2Price == null) {
                return stockData1;
            }

            return stock1Price.compareTo(stock2Price) > 0 ? stockData1 : stockData2;
        });
    }

    /**
     * 取得區間最低價
     *
     * @param stockDataFlux 股價Flux
     * @return Mono<StockData>
     */
    private Mono<StockData> getLowestStockData(Flux<StockData> stockDataFlux) {
        return stockDataFlux.reduce((stockData1, stockData2) -> {
            BigDecimal stock1Price = getStockDataLowest(stockData1);
            BigDecimal stock2Price = getStockDataLowest(stockData2);
            stockData1.setLowestPrice(stock1Price);
            stockData2.setLowestPrice(stock2Price);

            if (stock1Price == null) {
                return stockData2;
            }
            if (stock2Price == null) {
                return stockData1;
            }

            return stock1Price.compareTo(stock2Price) < 0 ? stockData1 : stockData2;
        });
    }

    /**
     * 取得當日最低價
     *
     * @param stockData 股價bean
     * @return result
     */
    private BigDecimal getStockDataLowest(StockData stockData) {
        BigDecimal highestPrice = stockData.getHighestPrice();
        BigDecimal lowestPrice = stockData.getLowestPrice();
        BigDecimal openingPrice = stockData.getOpeningPrice();
        BigDecimal closingPrice = stockData.getClosingPrice();

        return Stream.of(highestPrice, lowestPrice, openingPrice, closingPrice)
                .filter(Objects::nonNull)
                .min(Comparator.naturalOrder())
                .orElse(null);
    }

    /**
     * 取得當日最高價
     *
     * @param stockData 股價bean
     * @return result
     */
    private BigDecimal getStockDataHighest(StockData stockData) {
        BigDecimal highestPrice = stockData.getHighestPrice();
        BigDecimal lowestPrice = stockData.getLowestPrice();
        BigDecimal openingPrice = stockData.getOpeningPrice();
        BigDecimal closingPrice = stockData.getClosingPrice();

        return Stream.of(highestPrice, lowestPrice, openingPrice, closingPrice)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);
    }


    /**
     * StockMAResult 封裝
     *
     * @param window StockDataList
     * @param code   股票代號
     * @return StockMAResult MA計算bean
     */
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

    /**
     * 實際計算MA
     *
     * @param window stockDataList
     * @param period 計算天數
     * @return 計算結果
     */
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
