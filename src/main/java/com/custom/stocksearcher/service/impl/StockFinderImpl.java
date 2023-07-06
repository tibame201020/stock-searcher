package com.custom.stocksearcher.service.impl;

import com.custom.stocksearcher.models.CodeParam;
import com.custom.stocksearcher.models.CompanyStatus;
import com.custom.stocksearcher.models.StockData;
import com.custom.stocksearcher.models.StockMonthData;
import com.custom.stocksearcher.provider.DateProvider;
import com.custom.stocksearcher.repo.CompanyStatusRepo;
import com.custom.stocksearcher.repo.StockMonthDataRepo;
import com.custom.stocksearcher.repo.TPExStockRepo;
import com.custom.stocksearcher.service.StockFinder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Service
public class StockFinderImpl implements StockFinder {

    @Autowired
    private StockMonthDataRepo stockMonthDataRepo;
    @Autowired
    private CompanyStatusRepo companyStatusRepo;
    @Autowired
    private TPExStockRepo tpExStockRepo;
    @Autowired
    private DateProvider dateProvider;


    @Override
    public Flux<StockData> findStockInfo(CodeParam codeParam) {
        Flux<StockData> tpexStockDataFlux = findTPExStock(codeParam);
        Flux<StockData> listedStockDataFlux = findListedStock(codeParam);

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
                .filter(this::verifyStockData)
                .sort(Comparator.comparing(StockData::getDate));
    }

    @Override
    public Flux<CompanyStatus> findCompaniesByKeyWord(String keyword) {
        if (null == keyword || keyword.length() < 2) {
            return Flux.empty();
        }
        return companyStatusRepo
                .findAll()
                .filter(companyStatus -> {
                    if (companyStatus.isTPE()) {
                        return companyStatus.getCode().length() != 6;
                    } else {
                        return true;
                    }
                })
                .flatMap(companyStatus -> {
                    companyStatus.setUpdateDate(null);
                    return Mono.just(companyStatus);
                })
                .filter(companyStatus -> companyStatus.toString().contains(keyword));
    }


    /**
     * 上市股票查詢
     *
     * @param codeParam
     * @return
     */
    private Flux<StockData> findListedStock(CodeParam codeParam) {
        LocalDate beginDate = LocalDate.parse(codeParam.getBeginDate()).withDayOfMonth(1);
        LocalDate endDate = LocalDate.parse(codeParam.getEndDate()).withDayOfMonth(1);

        List<YearMonth> monthList = dateProvider.calculateMonthList(beginDate, endDate);
        return companyStatusRepo
                .findAllById(Collections.singleton(codeParam.getCode()))
                .filter(companyStatus -> !companyStatus.isTPE())
                .flatMap(companyStatus -> Flux.fromIterable(monthList).flatMap(month -> processMonth(companyStatus.getCode(), month)))
                .flatMap(stockMonthData -> Flux.fromIterable(stockMonthData.getStockDataList()));
    }

    /**
     * 根據month查詢elasticsearch 若無資料 則從crawler要資料
     *
     * @param stockCode 股票代號
     * @param month     年月份
     * @return 條件當月股價資訊集合
     */
    private Flux<StockMonthData> processMonth(String stockCode, YearMonth month) {
        YearMonth currentMonth = YearMonth.now();
        Flux<StockMonthData> existingData;
        if (month.equals(currentMonth)) {
            existingData = stockMonthDataRepo
                    .findByCodeAndYearMonthAndIsHistoryAndUpdateDate(stockCode, month.toString(), false, LocalDate.now());
        } else {
            existingData = stockMonthDataRepo.findByCodeAndYearMonthAndIsHistory(stockCode, month.toString(), true);
        }

        return existingData;
    }

    /**
     * 上櫃股票查詢
     *
     * @param codeParam
     * @return
     */
    private Flux<StockData> findTPExStock(CodeParam codeParam) {
        return companyStatusRepo
                .findAllById(Collections.singleton(codeParam.getCode()))
                .filter(CompanyStatus::isTPE)
                .filter(companyStatus -> companyStatus.getCode().length() != 6)
                .flatMap(companyStatus -> tpExStockRepo
                        .findByTpExStockId_CodeAndDateBetween(
                                companyStatus.getCode(),
                                LocalDate.parse(codeParam.getBeginDate()),
                                LocalDate.parse(codeParam.getEndDate())
                        )).
                flatMap(tpExStock -> Flux.just(tpExStock.getStockData()));
    }

    /**
     * 確保stockData為有效資料
     *
     * @param stockData
     * @return
     */
    private boolean verifyStockData(StockData stockData) {
        return stockData.getOpeningPrice() != null
                && stockData.getClosingPrice() != null
                && stockData.getHighestPrice() != null
                && stockData.getLowestPrice() != null;
    }

}
