package com.stocat.asset.websocket.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.reactive.config.EnableWebFlux;

@EnableWebFlux
@SpringBootApplication(
        scanBasePackages = {
                "com.stocat.asset.websocket.api",
                "com.stocat.common.redis",
        }
)
public class AssetWebsocketApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(AssetWebsocketApiApplication.class, args);
    }

}
