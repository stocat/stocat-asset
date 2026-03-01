package com.stocat.asset.websocket.api.config;

import com.stocat.asset.websocket.api.websocket.CryptoWebSocketHandler;
import com.stocat.asset.websocket.api.websocket.ExchangeRateWebSocketHandler;
import com.stocat.asset.websocket.api.websocket.KrStockWebSocketHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;

import java.util.Map;

@Configuration
public class WebSocketConfig {

    @Bean
    public HandlerMapping webSocketMapping(CryptoWebSocketHandler cryptoHandler,
            ExchangeRateWebSocketHandler exchangeRateHandler,
            KrStockWebSocketHandler krStockHandler) {
        return new SimpleUrlHandlerMapping(
                Map.of(
                        "/ws/crypto/trades", cryptoHandler,
                        "/ws/exchange-rates", exchangeRateHandler,
                        "/ws/kr-stock/trades", krStockHandler),
                Ordered.HIGHEST_PRECEDENCE);
    }

    @Bean
    public WebSocketHandlerAdapter handlerAdapter() {
        return new WebSocketHandlerAdapter();
    }
}
