package com.custom.stocksearcher.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.CorsRegistry;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.WebFluxConfigurer;

import static com.custom.stocksearcher.constant.Constant.CORS_URLS;

@EnableWebFlux
@Configuration
public class WebConfig implements WebFluxConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/stocks/**")
                .allowedOrigins(CORS_URLS)
                .allowedMethods("GET", "POST", "OPTIONS", "PUT", "DELETE", "PATCH")
                .allowCredentials(true);
    }
}
