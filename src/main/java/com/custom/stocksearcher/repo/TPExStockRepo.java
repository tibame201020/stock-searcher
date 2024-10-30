package com.custom.stocksearcher.repo;

import com.custom.stocksearcher.models.tpex.TPExStock;
import com.custom.stocksearcher.models.tpex.TPExStockId;
import org.springframework.data.elasticsearch.repository.ReactiveElasticsearchRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface TPExStockRepo extends ReactiveElasticsearchRepository<TPExStock, TPExStockId> {
    Mono<TPExStock> findFirstByOrderByDateDesc();
}
