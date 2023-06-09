package com.custom.stocksearcher;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class StockSearcherApplication {

    public static void main(String[] args) {
        SpringApplication.run(StockSearcherApplication.class, args);
    }

}
