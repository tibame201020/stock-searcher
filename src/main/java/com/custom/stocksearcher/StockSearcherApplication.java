package com.custom.stocksearcher;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.elasticsearch.repository.config.EnableReactiveElasticsearchRepositories;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.util.Objects;

@SpringBootApplication
@EnableReactiveElasticsearchRepositories
public class StockSearcherApplication {

    public static void main(String[] args) {
        SpringApplication.run(StockSearcherApplication.class, args);
    }

    @Bean
    public WebClient createWebClient() {
        return WebClient
                .builder()
                .clientConnector(new ReactorClientHttpConnector(HttpClient.create().followRedirect(true)))
                .exchangeStrategies(ExchangeStrategies
                        .builder()
                        .codecs(config -> config.defaultCodecs().maxInMemorySize(1048576 * 2000))
                        .build())
                .build();
    }

    @Bean
    public RestClient createElasticsearchClient() {
        String es_host = System.getenv("ES_HOST");
        if (Objects.isNull(es_host) || es_host.isEmpty()) {
            es_host = "172.23.214.95";
        }
        HttpHost host = new HttpHost(es_host, 9200);
        return RestClient.builder(host).build();
    }

}
