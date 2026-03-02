package com.stocat.asset.scraper.kr_stock.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stocat.asset.redis.constants.StockKeys;
import com.stocat.asset.scraper.messaging.event.TradeInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class KrStockRedisPublisher {

    private final ReactiveStringRedisTemplate redisTemplate;
    private final ObjectMapper mapper;

    public Mono<Void> publishTrade(TradeInfo tradeInfo) {
        return Mono.fromCallable(() -> mapper.writeValueAsString(tradeInfo))
                .flatMap(payload -> redisTemplate.convertAndSend(StockKeys.STOCK_TRADES, payload)
                        .doOnSuccess(count -> log.debug("KR 주식 체결 발행 - symbol={} listeners={}", tradeInfo.code(), count))
                )
                .onErrorResume(JsonProcessingException.class, e -> {
                    log.warn("KR 주식 체결 직렬화 실패 trade={}", tradeInfo, e);
                    return Mono.empty();
                })
                .then();
    }
}
