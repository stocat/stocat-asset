package com.stocat.asset.scraper.crypto.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;

@Configuration
@RequiredArgsConstructor
public class WebClientConfig {

    @Bean
    public ReactorNettyWebSocketClient webSocketClient() {
        return new ReactorNettyWebSocketClient();
    }
}
