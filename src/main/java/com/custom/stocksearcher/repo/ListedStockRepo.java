package com.custom.stocksearcher.repo;

import com.custom.stocksearcher.models.listed.ListedStock;
import com.custom.stocksearcher.models.listed.ListedStockId;
import org.springframework.data.elasticsearch.repository.ReactiveElasticsearchRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface ListedStockRepo extends ReactiveElasticsearchRepository<ListedStock, ListedStockId> {
    Mono<ListedStock> findFirstByListedStockId_CodeOrderByDateDesc(String code);

    Mono<ListedStock> findFirstByListedStockId_CodeOrderByDate(String code);
}
