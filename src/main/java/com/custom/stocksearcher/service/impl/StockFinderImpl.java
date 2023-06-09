package com.custom.stocksearcher.service.impl;

import com.custom.stocksearcher.models.StockData;
import com.custom.stocksearcher.provider.DateProvider;
import com.custom.stocksearcher.repo.StockDataRepo;
import com.custom.stocksearcher.service.StockCrawler;
import com.custom.stocksearcher.service.StockFinder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class StockFinderImpl implements StockFinder {

    @Autowired
    private StockDataRepo stockDataRepo;
    @Autowired
    private StockCrawler stockCrawler;
    @Autowired
    private DateProvider dateProvider;

    @Override
    public Flux<StockData> findStock(String stockCode, String begin, String end) {
        LocalDate beginDate = LocalDate.parse(begin).withDayOfMonth(1);
        LocalDate endDate = LocalDate.parse(end).withDayOfMonth(1).plusMonths(1);

        List<YearMonth> monthList = dateProvider.calculateMonthList(beginDate, endDate);

        return Flux.fromIterable(monthList)
                .flatMap(month -> processMonth(stockCode, month));
    }

    /**
     * 根據month查詢elasticsearch 若無資料 則從crawler要資料
     *
     * @param stockCode 股票代號
     * @param month     年月份
     * @return 條件當月股價資訊集合
     */
    private Flux<StockData> processMonth(String stockCode, YearMonth month) {
        YearMonth currentMonth = YearMonth.now();
        Flux<StockData> existingData;
        if (month.equals(currentMonth)) {
            existingData = stockDataRepo
                    .findByCodeAndMonthAndUpdateDateAndIsHistory(stockCode, month.toString(), LocalDate.now(), false);
        } else {
            existingData = stockDataRepo.findByCodeAndMonthAndIsHistory(stockCode, month.toString(), true);
        }

        Flux<StockData> twseStockData = Flux.defer(() ->
                stockCrawler.getStockDataLs(stockCode, month.atDay(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"))));

        return existingData.switchIfEmpty(twseStockData);
    }

}
