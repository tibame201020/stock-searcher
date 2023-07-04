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
import java.time.LocalDate;
import java.util.Collections;
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
    private CompanyStatusRepo companyStatusRepo;
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

        Flux<StockData> tpexStockDataFlux = stockFinder.findTPExStock(codeParam);


        Flux<StockData> listedStockDataFlux = stockFinder
                .findStock(codeParam.getCode(), codeParam.getBeginDate(), codeParam.getEndDate())
                .flatMap(stockMonthData -> Flux.fromIterable(stockMonthData.getStockDataList()));

        return companyStatusRepo.findAllById(Collections.singleton(codeParam.getCode()))
                .flatMap(companyStatus -> {
                    if (companyStatus.isTPE()) {
                        return tpexStockDataFlux;
                    } else {
                        return listedStockDataFlux;
                    }
                })
                .filter(stockData -> stockData.getDate().isBefore(LocalDate.now()))
                .filter(stockData ->
                        stockData.getDate().isAfter(LocalDate.parse(codeParam.getBeginDate()).minusDays(1))
                                && stockData.getDate().isBefore(LocalDate.parse(codeParam.getEndDate()).plusDays(1))
                )
                .sort(Comparator.comparing(StockData::getDate))
                .filter(stockData -> stockData.getOpeningPrice() != null
                        && stockData.getClosingPrice() != null
                        && stockData.getHighestPrice() != null
                        && stockData.getLowestPrice() != null);
    }

    @RequestMapping("/findCompaniesByKeyWord")
    public Flux<CompanyStatus> findCompaniesByKeyWord(@RequestBody String code) {
        return stockFinder.findCompaniesByKeyWord(code);
    }

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

        Flux<StockData> finalStockDataFlux = Flux.from(stockDataFlux);

        return stockCalculator.getRangeOfHighAndLowPoint(finalStockDataFlux, codeParam)
                .filter(stockBumpy ->
                        stockBumpy.getLowestTradeVolume().compareTo(codeParam.getTradeVolumeLimit()) >= 0
                ).flatMap(stockBumpy ->
                        finalStockDataFlux.sort(Comparator.comparing(StockData::getDate)).elementAt(0).flatMap(
                                stockData -> {
                                    stockBumpy.setBeginDate(stockData.getDate().toString());
                                    return Mono.just(stockBumpy);
                                }
                        )
                ).flatMap(stockBumpy ->
                        finalStockDataFlux.sort(Comparator.comparing(StockData::getDate)).last().flatMap(
                                stockData -> {
                                    stockBumpy.setEndDate(stockData.getDate().toString());
                                    return Mono.just(stockBumpy);
                                }
                        )
                );
    }

    @RequestMapping("/getAllRangeOfHighAndLowPoint")
    public Flux<StockBumpy> getAllRangeOfHighAndLowPoint(@RequestBody CodeParam codeParam) {
        BigDecimal bumpyHighLimit = codeParam.getBumpyHighLimit();
        BigDecimal bumpyLowLimit = codeParam.getBumpyLowLimit();

        if (null != codeParam.getBeforeEndDateDays() && codeParam.getBeforeEndDateDays() > 0) {
            LocalDate beginDate = LocalDate.parse(codeParam.getEndDate()).minusDays(codeParam.getBeforeEndDateDays());
            codeParam.setBeginDate(beginDate.toString());
        }
        Flux<CompanyStatus> companyStatusFlux;
        if ("all".equalsIgnoreCase(codeParam.getCode())) {
            companyStatusFlux = companyStatusRepo.findAll();
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

    @RequestMapping("getStockMa")
    public Flux<StockMAResult> getStockMa(@RequestBody CodeParam codeParam) {
        LocalDate beginDate = LocalDate.parse(codeParam.getBeginDate()).minusDays(1);
        LocalDate endDate = LocalDate.parse(codeParam.getEndDate()).plusDays(1);
        codeParam.setBeginDate(LocalDate.parse(codeParam.getBeginDate()).minusMonths(5).toString());
        return stockCalculator.getStockMa(findStockInfo(codeParam), codeParam.getCode())
                .filter(stockMAResult -> stockMAResult.getDate().isBefore(LocalDate.now()))
                .filter(stockMAResult -> stockMAResult.getDate().isAfter(beginDate) && stockMAResult.getDate().isBefore(endDate))
                .sort(Comparator.comparing(StockMAResult::getDate));
    }

    @RequestMapping("saveCodeList")
    public Flux<CodeList> saveCodeList(@RequestBody CodeList codeList) {
        return userStorage.saveCodeList(codeList);
    }

    @RequestMapping("getCodeListByUser")
    public Flux<CodeList> getCodeListByUser(@RequestBody String user) {
        return userStorage.getCodeListByUser(user);
    }

    @RequestMapping("getCodeList")
    public Mono<CodeList> getCodeList(@RequestBody String codeListId) {
        return codeListRepo.findById(codeListId);
    }

    @RequestMapping("deleteCodeList")
    public Mono<Void> deleteCodeList(@RequestBody String codeListId) {
        return codeListRepo.deleteById(codeListId);
    }

    @RequestMapping("getIntersectionFromCodeList")
    public Flux<CompanyStatus> getIntersectionFromCodeList(@RequestBody List<String> codeListIds) {
        return userStorage.getIntersectionFromCodeList(codeListIds);
    }

}
