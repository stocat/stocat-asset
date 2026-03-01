package com.stocat.asset.scraper.kr_stock.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "kr-stock")
@Getter
@Setter
public class KrStockProperties {

    /**
     * 기능 on/off 토글. 기본값은 비활성화.
     */
    private boolean enabled = false;

    /**
     * 동시에 구독할 KR 종목 수.
     */
    private int subscribeLimit = 5;

    private final KisProperties kis = new KisProperties();

    @Getter
    @Setter
    public static class KisProperties {
        private String clientId;
        private String clientSecret;
        private String websocketUrl;
        private String authBaseUrl;
        private List<String> symbols = new ArrayList<>();
    }
}
