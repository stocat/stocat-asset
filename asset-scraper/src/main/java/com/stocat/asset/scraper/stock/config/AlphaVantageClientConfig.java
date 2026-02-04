package com.stocat.asset.scraper.stock.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(AlphaVantageApiProperties.class)
public class AlphaVantageClientConfig {

    @Bean
    @Qualifier("alphaVantageWebClient")
    public WebClient alphaVantageWebClient(WebClient.Builder builder,
                                           AlphaVantageApiProperties properties) {
        return builder
                .baseUrl(properties.getBaseUrl())
                .defaultHeader("Accept", "application/json")
                .build();
    }
}
