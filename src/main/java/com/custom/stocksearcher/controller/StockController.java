package com.custom.stocksearcher.controller;

import com.custom.stocksearcher.models.*;
import com.custom.stocksearcher.repo.CodeListRepo;
import com.custom.stocksearcher.service.StockCalculator;
import com.custom.stocksearcher.service.StockFinder;
import com.custom.stocksearcher.service.UserStorage;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * 處理stock相關
 */
@RestController
@RequestMapping("/stocks")
public class StockController {
    private final Log log = LogFactory.getLog(this.getClass());

    private final StockFinder stockFinder;
    private final StockCalculator stockCalculator;
    private final UserStorage userStorage;
    private final CodeListRepo codeListRepo;

    public StockController(StockFinder stockFinder, StockCalculator stockCalculator, UserStorage userStorage, CodeListRepo codeListRepo) {
        this.stockFinder = stockFinder;
        this.stockCalculator = stockCalculator;
        this.userStorage = userStorage;
        this.codeListRepo = codeListRepo;
    }

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
                    CodeParam stockMAParam = new CodeParam();
                    stockMAParam.setCode(codeParam.getCode());
                    stockMAParam.setBeginDate(stockBumpy.getEndDate());
                    stockMAParam.setEndDate(stockBumpy.getEndDate());
                    return getStockMa(stockMAParam).last(new StockMAResult())
                            .filter(stockMAResult -> null != stockMAResult.getPrice())
                            .flatMap(stockMAResult -> filterClosingPriceWithMaPrice(stockMAResult, stockBumpy, codeParam.getClosingPriceCompareTargetHigher(),codeParam.getClosingPriceCompareTargetLower()));
                });
    }


    /**
     * 過濾收盤價格需高於或低於季線
     * @param stockMAResult 季線價格 bean
     * @param stockBumpy 計算結果
     * @param maTargetHigher 需高於哪個季線
     * @param maTargetLower 需低於哪個季線
     * @return
     */
    private Mono<StockBumpy> filterClosingPriceWithMaPrice(StockMAResult stockMAResult, StockBumpy stockBumpy, String maTargetHigher, String maTargetLower) {
        BigDecimal price = stockMAResult.getPrice();
        Optional<BigDecimal> maHigher = Optional.ofNullable(getMaTarget(stockMAResult, maTargetHigher));
        Optional<BigDecimal> maLower = Optional.ofNullable(getMaTarget(stockMAResult, maTargetLower));

        boolean isValid = maHigher.map(higher -> price.compareTo(higher) >= 0).orElse(true) &&
                maLower.map(lower -> price.compareTo(lower) <= 0).orElse(true);

        if (isValid) {
            stockBumpy.setLastStockMA(stockMAResult);
            return Mono.just(stockBumpy);
        }

        return Mono.empty();
    }

    /**
     * 取得要比較的 ma price
     * @param stockMAResult 季線價格 bean
     * @param maTarget
     * @return
     */
    private BigDecimal getMaTarget(StockMAResult stockMAResult, String maTarget) {
        BigDecimal ma5 = stockMAResult.getMa5();
        BigDecimal ma10 = stockMAResult.getMa10();
        BigDecimal ma20 = stockMAResult.getMa20();
        BigDecimal ma60 = stockMAResult.getMa60();

        return switch (maTarget) {
            case "none" -> null;
            case "MA5" -> Optional.ofNullable(ma5).orElse(BigDecimal.ZERO);
            case "MA10" -> Optional.ofNullable(ma10).orElse(BigDecimal.ZERO);
            case "MA20" -> Optional.ofNullable(ma20).orElse(BigDecimal.ZERO);
            case "MA60" -> Optional.ofNullable(ma60).orElse(BigDecimal.ZERO);
            default -> BigDecimal.ZERO;
        };
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

        Flux<CompanyStatus> companyStatusFlux = userStorage.getCodeRange(codeParam.getCode(), codeParam.isWithout4upCode());
        Flux<CodeParam> codeParamFlux = userStorage.wrapperCodeParam(companyStatusFlux, codeParam);

        return codeParamFlux
                .parallel()
                .runOn(Schedulers.parallel())
                .flatMap(this::getRangeOfHighAndLowPoint)
                .sequential()
                .filter(stockBumpy -> {
                    if (bumpyHighLimit.compareTo(BigDecimal.ZERO) != 0) {
                        return stockBumpy.getCalcResult().compareTo(bumpyHighLimit) < 0;
                    } else {
                        return true;
                    }
                })
                .filter(stockBumpy -> stockBumpy.getCalcResult().compareTo(bumpyLowLimit) >= 0)
                .sort(Comparator.comparing(stockBumpy -> stockBumpy.getLastStockMA().getPrice().negate()));
    }

    /**
     * 取得股價MA
     *
     * @param codeParam 查詢bean
     * @return 計算結果
     */
    @RequestMapping("/getStockMa")
    public Flux<StockMAResult> getStockMa(@RequestBody CodeParam codeParam) {
        LocalDate beginDate = LocalDate.parse(codeParam.getBeginDate()).minusDays(1);
        LocalDate endDate = LocalDate.parse(codeParam.getEndDate()).plusDays(1);
        codeParam.setBeginDate(beginDate.minusMonths(6).toString());

        return stockCalculator.getStockMa(findStockInfo(codeParam), codeParam.getCode(), beginDate, endDate);
    }

    /**
     * 儲存CodeList
     *
     * @param codeList
     * @return
     */
    @RequestMapping("/saveCodeList")
    public Flux<CodeList> saveCodeList(@RequestBody CodeList codeList) {
        return userStorage.saveCodeList(codeList);
    }

    /**
     * 取得用戶CodeList
     *
     * @param user
     * @return
     */
    @RequestMapping("/getCodeListByUser")
    public Flux<CodeList> getCodeListByUser(@RequestBody String user) {
        return userStorage.getCodeListByUser(user);
    }

    /**
     * 根據id取得CodeList
     *
     * @param codeListId
     * @return
     */
    @RequestMapping("/getCodeList")
    public Mono<CodeList> getCodeList(@RequestBody String codeListId) {
        return codeListRepo.findById(codeListId);
    }

    /**
     * 刪除CodeList
     *
     * @param codeListId
     * @return
     */
    @RequestMapping("/deleteCodeList")
    public Mono<Void> deleteCodeList(@RequestBody String codeListId) {
        return codeListRepo.deleteById(codeListId);
    }

    /**
     * 取得複數CodeList的交集
     *
     * @param codeListIds
     * @return
     */
    @RequestMapping("/getIntersectionFromCodeList")
    public Flux<CompanyStatus> getIntersectionFromCodeList(@RequestBody List<String> codeListIds) {
        return userStorage.getIntersectionFromCodeList(codeListIds);
    }

    @RequestMapping("/getAllCandlestickType")
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
