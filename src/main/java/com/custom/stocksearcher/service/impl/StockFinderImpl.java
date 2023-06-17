package com.custom.stocksearcher.service.impl;

import com.custom.stocksearcher.models.CompanyStatus;
import com.custom.stocksearcher.models.StockMonthData;
import com.custom.stocksearcher.provider.DateProvider;
import com.custom.stocksearcher.repo.CompanyStatusRepo;
import com.custom.stocksearcher.repo.StockMonthDataRepo;
import com.custom.stocksearcher.service.StockCrawler;
import com.custom.stocksearcher.service.StockFinder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class StockFinderImpl implements StockFinder {

    @Autowired
    private StockMonthDataRepo stockMonthDataRepo;
    @Autowired
    private CompanyStatusRepo companyStatusRepo;
    @Autowired
    private StockCrawler stockCrawler;
    @Autowired
    private DateProvider dateProvider;


    @Override
    public Flux<StockMonthData> findStock(String stockCode, String begin, String end) {
        LocalDate beginDate = LocalDate.parse(begin).withDayOfMonth(1);
        LocalDate endDate = LocalDate.parse(end).withDayOfMonth(1).plusMonths(1);

        List<YearMonth> monthList = dateProvider.calculateMonthList(beginDate, endDate);

        return Flux.fromIterable(monthList)
                .flatMap(month -> processMonth(stockCode, month));
    }

    @Override
    public Flux<CompanyStatus> findCompaniesByKeyWord(String keyword) {
        if (keyword == null || keyword.length() < 2) {
            return null;
        }
        return companyStatusRepo
                .findAll()
                .flatMap(companyStatus -> {
                    companyStatus.setUpdateDate(null);
                    return Mono.just(companyStatus);
                })
                .filter(companyStatus -> companyStatus.toString().contains(keyword));
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

        Flux<StockMonthData> twseStockData = Flux.defer(() ->
                stockCrawler.getStockMonthDataFromTWSEApi(stockCode, month.atDay(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"))));

        return existingData.switchIfEmpty(twseStockData);
    }

}
