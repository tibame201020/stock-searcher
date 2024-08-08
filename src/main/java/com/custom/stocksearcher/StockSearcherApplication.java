package com.custom.stocksearcher;

import com.custom.stocksearcher.config.LocalDateTypeAdapter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.elasticsearch.repository.config.EnableReactiveElasticsearchRepositories;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.LocalDate;
import java.util.Objects;

@SpringBootApplication
@EnableScheduling
@EnableReactiveElasticsearchRepositories
public class StockSearcherApplication {

    public static void main(String[] args) {
        SpringApplication.run(StockSearcherApplication.class, args);
    }

    @Bean
    public Gson getGson() {
        return new GsonBuilder()
                .registerTypeAdapter(LocalDate.class, new LocalDateTypeAdapter())
                .create();
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
            es_host = "localhost";
        }
        HttpHost host = new HttpHost(es_host, 9200);
        return RestClient.builder(host).build();
    }

}
