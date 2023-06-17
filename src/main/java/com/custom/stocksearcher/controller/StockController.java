package com.custom.stocksearcher.controller;

import com.custom.stocksearcher.models.CodeParam;
import com.custom.stocksearcher.models.CompanyStatus;
import com.custom.stocksearcher.models.StockData;
import com.custom.stocksearcher.service.StockFinder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.LocalDate;
import java.util.Comparator;

/**
 * 處理stock相關
 */
@RestController
@RequestMapping("/stocks")
public class StockController {

    private final Log log = LogFactory.getLog(this.getClass());

    @Autowired
    private StockFinder stockFinder;

    /**
     * 根據條件查詢單一股價
     *
     * @param codeParam 查詢條件bean
     * @return 股價資訊集合
     */
    @RequestMapping("/findStockInfo")
    public Flux<StockData> findStockInfo(@RequestBody CodeParam codeParam) {
        return stockFinder
                .findStock(codeParam.getCode(), codeParam.getBeginDate(), codeParam.getEndDate())
                .flatMap(stockMonthData -> Flux.fromIterable(stockMonthData.getStockDataList()))
                .filter(stockData ->
                        stockData.getDate().isAfter(LocalDate.parse(codeParam.getBeginDate()).minusDays(1))
                                && stockData.getDate().isBefore(LocalDate.parse(codeParam.getEndDate()).plusDays(1))
                )
                .sort(Comparator.comparing(StockData::getDate));
    }

    @RequestMapping("/findCompaniesByKeyWord")
    public Flux<CompanyStatus> findCompaniesByKeyWord(@RequestBody CodeParam codeParam) {
        return stockFinder.findCompaniesByKeyWord(codeParam.getCode());
    }

}
