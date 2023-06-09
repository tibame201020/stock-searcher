package com.custom.stocksearcher.repo;

import com.custom.stocksearcher.models.StockData;
import com.custom.stocksearcher.models.StockDataId;
import org.springframework.data.elasticsearch.repository.ReactiveElasticsearchRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.time.LocalDate;

@Repository
public interface StockDataRepo extends ReactiveElasticsearchRepository<StockData, StockDataId> {
    Flux<StockData> findByCodeAndMonthAndUpdateDateAndIsHistory(String code, String month, LocalDate update, boolean isHistory);
    Flux<StockData> findByCodeAndMonthAndIsHistory(String code, String month, boolean isHistory);
}
