package com.stocat.asset.scraper.stock.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "alpha-vantage.api")
public class AlphaVantageApiProperties {

    private String baseUrl = "https://www.alphavantage.co";
    private String apiKey;
}
