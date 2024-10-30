package com.custom.stocksearcher.repo;

import com.custom.stocksearcher.models.CompanyStatus;
import org.springframework.data.elasticsearch.repository.ReactiveElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CompanyStatusRepo extends ReactiveElasticsearchRepository<CompanyStatus, String> {
}
