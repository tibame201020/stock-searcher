package com.custom.stocksearcher;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.elasticsearch.repository.config.EnableReactiveElasticsearchRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableReactiveElasticsearchRepositories
public class StockSearcherApplication {

    public static void main(String[] args) {
        SpringApplication.run(StockSearcherApplication.class, args);
    }

    @Bean
    public RestClient createElasticsearchClient() {
        String es_host = System.getenv("ES_HOST");
        if (null == es_host || es_host.isEmpty()) {
            es_host = "localhost";
        }
        HttpHost host = new HttpHost(es_host, 9200);
        return RestClient.builder(host).build();
    }

}
