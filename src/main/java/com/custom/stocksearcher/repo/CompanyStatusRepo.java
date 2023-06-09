package com.custom.stocksearcher.repo;

import com.custom.stocksearcher.models.CompanyStatus;
import org.springframework.data.elasticsearch.repository.ReactiveElasticsearchRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

@Repository
public interface CompanyStatusRepo extends ReactiveElasticsearchRepository<CompanyStatus, String> {
    Flux<CompanyStatus> findByWasCrawler(boolean wasCrawler);
    Flux<CompanyStatus> findByUpdateDate(LocalDate updateDate);
}
