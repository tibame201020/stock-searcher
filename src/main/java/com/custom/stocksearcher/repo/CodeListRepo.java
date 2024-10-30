package com.custom.stocksearcher.repo;

import com.custom.stocksearcher.models.CodeList;
import org.springframework.data.elasticsearch.repository.ReactiveElasticsearchRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface CodeListRepo extends ReactiveElasticsearchRepository<CodeList, String> {
    Flux<CodeList> findByUser(String user);
}
