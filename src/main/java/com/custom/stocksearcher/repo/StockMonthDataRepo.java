package com.custom.stocksearcher.repo;

import com.custom.stocksearcher.models.StockMonthData;
import com.custom.stocksearcher.models.StockMonthDataId;
import org.springframework.data.elasticsearch.repository.ReactiveElasticsearchRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.time.LocalDate;

@Repository
public interface StockMonthDataRepo extends ReactiveElasticsearchRepository<StockMonthData, StockMonthDataId> {
    Flux<StockMonthData> findByCodeAndYearMonthAndIsHistory(String code, String yearMonth, boolean isHistory);
    Flux<StockMonthData> findByCodeAndYearMonthAndIsHistoryAndUpdateDate(String code, String yearMonth, boolean isHistory, LocalDate updateDate);
}
