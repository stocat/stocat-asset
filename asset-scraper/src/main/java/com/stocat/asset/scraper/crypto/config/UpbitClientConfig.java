package com.stocat.asset.scraper.crypto.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(UpbitApiProperties.class)
public class UpbitClientConfig {

    @Bean
    @Qualifier("upbitWebClient")
    public WebClient upbitWebClient(WebClient.Builder builder,
                                    UpbitApiProperties upbitApiProperties) {
        return builder
                .baseUrl(upbitApiProperties.getRestBaseUrl())
                .defaultHeader("Accept", "application/json")
                .build();
    }
}
