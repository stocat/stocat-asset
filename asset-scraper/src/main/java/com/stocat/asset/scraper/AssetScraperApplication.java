package com.stocat.asset.scraper;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(
        scanBasePackages = {
                "com.stocat.asset.scraper",
                "com.stocat.asset.redis",
                "com.stocat.asset.mysql",
                "com.stocat.asset.core"
        }
)
public class AssetScraperApplication {

    public static void main(String[] args) {
        SpringApplication.run(AssetScraperApplication.class, args);
    }

}
