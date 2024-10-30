package com.custom.stocksearcher.repo;

import com.custom.stocksearcher.models.listed.ListedStock;
import com.custom.stocksearcher.models.listed.ListedStockId;
import org.springframework.data.elasticsearch.repository.ReactiveElasticsearchRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.time.LocalDate;

@Repository
public interface ListedStockRepo extends ReactiveElasticsearchRepository<ListedStock, ListedStockId> {
    Flux<ListedStock> findByListedStockId_CodeAndDateBetweenOrderByDate(String code, LocalDate begin, LocalDate end);
}
