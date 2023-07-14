package com.custom.stocksearcher.controller;

import com.custom.stocksearcher.models.*;
import com.custom.stocksearcher.repo.CodeListRepo;
import com.custom.stocksearcher.service.StockCalculator;
import com.custom.stocksearcher.service.StockFinder;
import com.custom.stocksearcher.service.UserStorage;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 處理stock相關
 */
@RestController
@RequestMapping("/stocks")
public class StockController {
    private final Log log = LogFactory.getLog(this.getClass());
    @Autowired
    private StockFinder stockFinder;
    @Autowired
    private StockCalculator stockCalculator;
    @Autowired
    private UserStorage userStorage;
    @Autowired
    private CodeListRepo codeListRepo;

    /**
     * 根據條件查詢單一股價
     *
     * @param codeParam 查詢條件bean
     * @return 股價資訊集合
     */
    @RequestMapping("/findStockInfo")
    public Flux<StockData> findStockInfo(@RequestBody CodeParam codeParam) {
        return stockFinder.findStockInfo(codeParam);
    }

    /**
     * 根據keyword尋找companies
     *
     * @param code 部分關鍵字
     * @return Flux<CompanyStatus>
     */
    @RequestMapping("/findCompaniesByKeyWord")
    public Flux<CompanyStatus> findCompaniesByKeyWord(@RequestBody String code) {
        return stockFinder.findCompaniesByKeyWord(code);
    }

    /**
     * calc stock
     *
     * @param codeParam 查詢bean
     * @return Mono<StockBumpy>
     */
    @RequestMapping("/getRangeOfHighAndLowPoint")
    public Mono<StockBumpy> getRangeOfHighAndLowPoint(@RequestBody CodeParam codeParam) {
        Flux<StockData> stockDataFlux = stockFinder.getStockDataWithKlineCnt(codeParam);
        stockDataFlux = stockCalculator.preFilterLastStockData(stockDataFlux, codeParam);

        return stockCalculator.getRangeOfHighAndLowPoint(stockDataFlux, codeParam)
                .filter(stockBumpy -> stockBumpy.getLowestTradeVolume().compareTo(codeParam.getTradeVolumeLimit()) >= 0)
                .flatMap(stockBumpy -> {
                    if ("none".equalsIgnoreCase(codeParam.getClosingPriceCompareTarget())) {
                        return Mono.just(stockBumpy);
                    }

                    CodeParam stockMAParam = new CodeParam();
                    stockMAParam.setCode(codeParam.getCode());
                    stockMAParam.setBeginDate(stockBumpy.getEndDate());
                    stockMAParam.setEndDate(stockBumpy.getEndDate());
                    return getStockMa(stockMAParam).last().flatMap(stockMAResult -> {
                        BigDecimal price = stockMAResult.getPrice();
                        BigDecimal ma5 = stockMAResult.getMa5();
                        BigDecimal ma10 = stockMAResult.getMa10();
                        BigDecimal ma20 = stockMAResult.getMa20();
                        BigDecimal ma60 = stockMAResult.getMa60();
                        BigDecimal ma = BigDecimal.valueOf(-1);

                        switch (codeParam.getClosingPriceCompareTarget()) {
                            case "MA5" ->
                                    ma = ma5 != null ? ma5 : BigDecimal.ZERO;
                            case "MA10" ->
                                    ma = ma10 != null ? ma10 : ma5 != null ? ma5 : BigDecimal.ZERO;
                            case "MA20" ->
                                    ma = ma20 != null ? ma20 : ma10 != null ? ma10 : ma5 != null ? ma5 : BigDecimal.ZERO;
                            case "MA60" ->
                                    ma = ma60 != null ? ma60 : ma20 != null ? ma20 : ma10 != null ? ma10 : ma5 != null ? ma5 : BigDecimal.ZERO;
                        }

                        if (price.compareTo(ma) >= 0) {
                            stockBumpy.setLastStockMA(stockMAResult);
                            return Mono.just(stockBumpy);
                        } else {
                            return Mono.empty();
                        }
                    });
                });
    }

    /**
     * calc by all or a range
     *
     * @param codeParam 查詢bean
     * @return FLux<StockBumpy>
     */
    @RequestMapping("/getAllRangeOfHighAndLowPoint")
    public Flux<StockBumpy> getAllRangeOfHighAndLowPoint(@RequestBody CodeParam codeParam) {
        BigDecimal bumpyHighLimit = codeParam.getBumpyHighLimit();
        BigDecimal bumpyLowLimit = codeParam.getBumpyLowLimit();

        Flux<CompanyStatus> companyStatusFlux = userStorage.getCodeRange(codeParam.getCode());
        Flux<CodeParam> codeParamFlux = userStorage.wrapperCodeParam(companyStatusFlux, codeParam);

        return codeParamFlux
                .flatMap(this::getRangeOfHighAndLowPoint)
                .filter(stockBumpy -> {
                    if (bumpyHighLimit.compareTo(BigDecimal.ZERO) != 0) {
                        return stockBumpy.getCalcResult().compareTo(bumpyHighLimit) < 0;
                    } else {
                        return true;
                    }
                })
                .filter(stockBumpy -> stockBumpy.getCalcResult().compareTo(bumpyLowLimit) >= 0)
                .sort(Comparator.comparing(stockBumpy -> stockBumpy.getCalcResult().negate()));
    }

    /**
     * 取得股價MA
     *
     * @param codeParam 查詢bean
     * @return 計算結果
     */
    @RequestMapping("getStockMa")
    public Flux<StockMAResult> getStockMa(@RequestBody CodeParam codeParam) {
        LocalDate beginDate = LocalDate.parse(codeParam.getBeginDate()).minusDays(1);
        LocalDate endDate = LocalDate.parse(codeParam.getEndDate()).plusDays(1);
        codeParam.setBeginDate(beginDate.minusMonths(3).toString());

        return stockCalculator.getStockMa(findStockInfo(codeParam), codeParam.getCode())
                .filter(stockMAResult -> stockMAResult.getDate().isAfter(beginDate) && stockMAResult.getDate().isBefore(endDate))
                .sort(Comparator.comparing(StockMAResult::getDate));
    }

    /**
     * 儲存CodeList
     *
     * @param codeList
     * @return
     */
    @RequestMapping("saveCodeList")
    public Flux<CodeList> saveCodeList(@RequestBody CodeList codeList) {
        return userStorage.saveCodeList(codeList);
    }

    /**
     * 取得用戶CodeList
     *
     * @param user
     * @return
     */
    @RequestMapping("getCodeListByUser")
    public Flux<CodeList> getCodeListByUser(@RequestBody String user) {
        return userStorage.getCodeListByUser(user);
    }

    /**
     * 根據id取得CodeList
     *
     * @param codeListId
     * @return
     */
    @RequestMapping("getCodeList")
    public Mono<CodeList> getCodeList(@RequestBody String codeListId) {
        return codeListRepo.findById(codeListId);
    }

    /**
     * 刪除CodeList
     *
     * @param codeListId
     * @return
     */
    @RequestMapping("deleteCodeList")
    public Mono<Void> deleteCodeList(@RequestBody String codeListId) {
        return codeListRepo.deleteById(codeListId);
    }

    /**
     * 取得複數CodeList的交集
     *
     * @param codeListIds
     * @return
     */
    @RequestMapping("getIntersectionFromCodeList")
    public Flux<CompanyStatus> getIntersectionFromCodeList(@RequestBody List<String> codeListIds) {
        return userStorage.getIntersectionFromCodeList(codeListIds);
    }

    @RequestMapping("getAllCandlestickType")
    public Flux<Map<String, String>> getAllCandlestickType() {
        return Flux
                .fromArray(CandlestickType.values())
                .map(candlestickType -> {
                    Map<String, String> candlestickTypeMap = new HashMap<>();
                    candlestickTypeMap.put("key", candlestickType.toString());
                    candlestickTypeMap.put("name", candlestickType.getName());
                    return candlestickTypeMap;
                });
    }

}
