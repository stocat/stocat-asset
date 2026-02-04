package com.stocat.asset.scraper.stock.service.quote;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties("stock.quote")
public class StockQuoteProperties {
    private String provider = "alpha";
}
