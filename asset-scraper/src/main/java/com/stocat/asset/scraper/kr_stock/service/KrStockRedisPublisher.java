package com.stocat.asset.scraper.kr_stock.service;

import com.stocat.asset.redis.constants.StockKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class KrStockRedisPublisher {

    private final ReactiveStringRedisTemplate redisTemplate;

    public Mono<Void> publishTrade(String symbol, BigDecimal price) {
        String payload = String.format(
                "{\"symbol\":\"%s\",\"price\":%s,\"ts\":\"%s\"}",
                symbol,
                price,
                Instant.now().toString()
        );

        return redisTemplate.convertAndSend(StockKeys.STOCK_TRADES, payload)
                .doOnSuccess(count -> log.debug("KR 주식 체결 발행 - symbol={} listeners={}", symbol, count))
                .then();
    }
}
