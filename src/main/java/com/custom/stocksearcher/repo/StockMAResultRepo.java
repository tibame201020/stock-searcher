package com.custom.stocksearcher.repo;

import com.custom.stocksearcher.models.StockMAResult;
import com.custom.stocksearcher.models.StockMAResultId;
import org.springframework.data.elasticsearch.repository.ReactiveElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StockMAResultRepo extends ReactiveElasticsearchRepository<StockMAResult, StockMAResultId> {
}
