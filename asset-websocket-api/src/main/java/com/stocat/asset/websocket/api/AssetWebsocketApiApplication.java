package com.stocat.asset.websocket.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.reactive.config.EnableWebFlux;

import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableWebFlux
@EnableDiscoveryClient
@SpringBootApplication(scanBasePackages = {
        "com.stocat.asset.websocket.api",
        "com.stocat.asset.redis",
})
public class AssetWebsocketApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(AssetWebsocketApiApplication.class, args);
    }

}
