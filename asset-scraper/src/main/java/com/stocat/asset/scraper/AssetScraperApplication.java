package com.stocat.asset.scraper;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(
        scanBasePackages = {
                "com.stocat.asset.scraper",
                "com.stocat.common.redis",
                "com.stocat.common"
        }
)
public class AssetScraperApplication {

    public static void main(String[] args) {
        SpringApplication.run(AssetScraperApplication.class, args);
    }

}
