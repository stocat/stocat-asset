package com.stocat.asset.scraper.kr_stock.service;

import com.stocat.asset.redis.constants.StockKeys;
import com.stocat.asset.scraper.kr_stock.config.KrStockProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KrStockSubscriptionService {

    private final ReactiveStringRedisTemplate redisTemplate;
    private final KrStockProperties properties;

    private final Sinks.Many<List<String>> sink = Sinks.many().replay().latest();

    @PostConstruct
    public void init() {
        if (!properties.isEnabled()) {
            log.info("KR 주식 구독 코드 서비스 비활성화 - kr-stock.enabled=false");
            return;
        }

        seedDefaultCodes()
                .onErrorResume(error -> {
                    log.error("KR 주식 기본 코드 초기화 실패", error);
                    return Mono.empty();
                })
                .then(reloadCodesMono())
                .subscribe();
    }

    public Flux<List<String>> codeFlux() {
        return sink.asFlux();
    }

    public void reloadCodes() {
        if (!properties.isEnabled()) {
            return;
        }
        reloadCodesMono().subscribe();
    }

    private Mono<Void> reloadCodesMono() {
        int limit = properties.getSubscribeLimit();
        return redisTemplate.opsForSet()
                .members(StockKeys.STOCK_SUBSCRIBE_CODES)
                .collectList()
                .map(values -> values.stream()
                        .filter(StringUtils::hasText)
                        .map(String::trim)
                        .distinct()
                        .sorted()
                        .limit(limit)
                        .collect(Collectors.toCollection(ArrayList::new))
                )
                .filter(list -> !list.isEmpty())
                .doOnNext(list -> log.info("Redis KR 주식 구독 코드 로드 완료 - {}", list))
                .doOnNext(sink::tryEmitNext)
                .then();
    }

    private Mono<Void> seedDefaultCodes() {
        List<String> configured = properties.getKis().getSymbols();
        if (configured == null || configured.isEmpty()) {
            log.warn("KR 주식 구독 기본 코드가 비어 있습니다. redis 등록을 건너뜁니다.");
            return Mono.empty();
        }

        int limit = properties.getSubscribeLimit();
        List<String> candidate = configured.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .limit(limit)
                .toList();

        if (candidate.size() < limit) {
            log.warn("KR 주식 구독 코드가 {}개 이하입니다. property에 {}개 이상 지정해주세요.", limit, limit);
        }

        if (candidate.isEmpty()) {
            return Mono.empty();
        }

        String[] codes = candidate.toArray(new String[0]);
        return redisTemplate.delete(StockKeys.STOCK_HOT_CODES)
                .then(redisTemplate.delete(StockKeys.STOCK_SUBSCRIBE_CODES))
                .then(redisTemplate.opsForSet().add(StockKeys.STOCK_HOT_CODES, codes))
                .then(redisTemplate.opsForSet().add(StockKeys.STOCK_SUBSCRIBE_CODES, codes))
                .doOnSubscribe(sub -> log.info("Redis KR 주식 기본 코드 {}개 등록", codes.length))
                .then();
    }
}
