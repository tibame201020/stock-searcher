package com.custom.stocksearcher.repo;

import com.custom.stocksearcher.models.tpex.TPExStock;
import com.custom.stocksearcher.models.tpex.TPExStockId;
import org.springframework.data.elasticsearch.repository.ReactiveElasticsearchRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.time.LocalDate;

@Repository
public interface TPExStockRepo extends ReactiveElasticsearchRepository<TPExStock, TPExStockId> {
    Flux<TPExStock> findByTpExStockId_CodeAndDateBetweenOrderByDate(String code, LocalDate begin, LocalDate end);
}
