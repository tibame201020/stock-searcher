package com.custom.stocksearcher.controller;

import com.custom.stocksearcher.models.*;
import com.custom.stocksearcher.repo.CodeListRepo;
import com.custom.stocksearcher.repo.CompanyStatusRepo;
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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

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
    private CompanyStatusRepo companyStatusRepo;
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
        Integer klineCnt = codeParam.getKlineCnt();
        if (null != klineCnt && klineCnt > 0) {
            LocalDate beginDate = LocalDate.parse(codeParam.getEndDate()).minusDays(klineCnt * 3L);
            codeParam.setBeginDate(beginDate.toString());
        }

        Flux<StockData> stockDataFlux = findStockInfo(codeParam);

        if (null != klineCnt && klineCnt > 0) {
            stockDataFlux = stockDataFlux.takeLast(klineCnt);
        }

        Flux<StockData> finalStockDataFlux = Flux.from(stockDataFlux).sort(Comparator.comparing(StockData::getDate));

        finalStockDataFlux = finalStockDataFlux
                .buffer()
                .flatMap(stockDataList -> {
                    StockData lastStockData = stockDataList.get(stockDataList.size() - 1);
                    BigDecimal openPrice = lastStockData.getOpeningPrice();
                    BigDecimal closingPrice = lastStockData.getClosingPrice();
                    BigDecimal lowestPrice = lastStockData.getLowestPrice();
                    BigDecimal lastOpenCalc = openPrice.subtract(lowestPrice).divide(lowestPrice, 4, RoundingMode.FLOOR).multiply(BigDecimal.valueOf(100));
                    BigDecimal lastCloseCalc = closingPrice.subtract(lowestPrice).divide(lowestPrice, 4, RoundingMode.FLOOR).multiply(BigDecimal.valueOf(100));

                    if ((lastOpenCalc.compareTo(codeParam.getLastOpenCalcLimit()) >= 0) && (lastCloseCalc.compareTo(codeParam.getLastCloseCalcLimit()) >= 0)) {
                        return Flux.fromIterable(stockDataList);
                    } else {
                        return Flux.empty();
                    }
                });

        return stockCalculator.getRangeOfHighAndLowPoint(finalStockDataFlux, codeParam)
                .filter(stockBumpy -> stockBumpy.getLowestTradeVolume().compareTo(codeParam.getTradeVolumeLimit()) >= 0)
                .flatMap(stockBumpy -> {
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

        Flux<CompanyStatus> companyStatusFlux;
        if ("all".equalsIgnoreCase(codeParam.getCode())) {
            companyStatusFlux = companyStatusRepo.findAll();
        } else if ("listed".equalsIgnoreCase(codeParam.getCode())) {
            companyStatusFlux = companyStatusRepo.findAll().filter(companyStatus -> !companyStatus.isTPE());
        } else if ("tpex".equalsIgnoreCase(codeParam.getCode())) {
            companyStatusFlux = companyStatusRepo.findAll().filter(CompanyStatus::isTPE);
        } else {
            companyStatusFlux = codeListRepo
                    .findById(codeParam.getCode())
                    .flux()
                    .flatMap(codeList -> Flux.fromIterable(codeList.getCodes()));
        }

        Flux<CodeParam> codeParamFlux = companyStatusFlux.flatMap(
                companyStatus -> {
                    CodeParam codeParam1 = new CodeParam();
                    codeParam1.setCode(companyStatus.getCode());
                    codeParam1.setBeginDate(codeParam.getBeginDate());
                    codeParam1.setEndDate(codeParam.getEndDate());
                    codeParam1.setTradeVolumeLimit(codeParam.getTradeVolumeLimit());
                    codeParam1.setKlineCnt(codeParam.getKlineCnt());
                    codeParam1.setLastOpenCalcLimit(codeParam.getLastOpenCalcLimit());
                    codeParam1.setLastCloseCalcLimit(codeParam.getLastCloseCalcLimit());
                    codeParam1.setClosingPriceCompareTarget(codeParam.getClosingPriceCompareTarget());

                    return Mono.just(codeParam1);
                }
        );
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
        codeParam.setBeginDate(beginDate.minusMonths(5).toString());

        return stockCalculator.getStockMa(findStockInfo(codeParam), codeParam.getCode())
                .filter(stockMAResult -> stockMAResult.getDate().isBefore(LocalDate.now()))
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

}
