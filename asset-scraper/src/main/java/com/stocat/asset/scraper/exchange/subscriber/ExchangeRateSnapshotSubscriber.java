package com.stocat.asset.scraper.exchange.subscriber;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stocat.asset.redis.constants.ExchangeRateKeys;
import com.stocat.asset.scraper.exchange.dto.ExchangeRateInfo;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.ReactiveSubscription;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * exchange:rates 채널을 구독하여 환율 스냅샷을 Redis에 저장하는 컴포넌트
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ExchangeRateSnapshotSubscriber {

    private final ReactiveRedisMessageListenerContainer redisContainer;
    private final ChannelTopic exchangeRatesTopic;
    private final ReactiveStringRedisTemplate redisTemplate;
    private final ObjectMapper mapper;

    @PostConstruct
    public void start() {
        redisContainer.receive(exchangeRatesTopic)
                .map(ReactiveSubscription.Message::getMessage)
                .flatMap(this::processSnapshot)
                .doOnError(err -> log.error("환율 스냅샷 구독 파이프라인 오류", err))
                .subscribe(null, err -> log.error("환율 스냅샷 구독 종료 (오류)", err));
    }

    private Mono<Void> processSnapshot(String json) {
        return Mono.fromCallable(() -> mapper.readValue(json, ExchangeRateInfo.class))
                .flatMap(dto -> saveSnapshot(dto).and(saveInverseSnapshot(dto)).then())
                .doOnError(e -> log.error("환율 스냅샷 파싱 실패 - json={}", json, e))
                .onErrorResume(e -> Mono.empty());
    }

    private Mono<Boolean> saveSnapshot(ExchangeRateInfo dto) {
        String key = ExchangeRateKeys.rateKey(dto.currencyPair());
        String value = dto.exchangeRate().toPlainString();
        return redisTemplate.opsForValue().set(key, value)
                .doOnSuccess(ok -> log.debug("Redis 환율 스냅샷 저장 - key={}, value={}", key, value));
    }

    private Mono<Boolean> saveInverseSnapshot(ExchangeRateInfo dto) {
        String inversePair = invertPair(dto.currencyPair());
        String key = ExchangeRateKeys.rateKey(inversePair);
        String value = inverseRate(dto.exchangeRate());
        return redisTemplate.opsForValue().set(key, value)
                .doOnSuccess(ok -> log.debug("Redis 역방향 환율 스냅샷 저장 - key={}, value={}", key, value));
    }

    private String invertPair(String currencyPair) {
        return switch (currencyPair) {
            case "USDKRW" -> "KRWUSD";
            case "KRWUSD" -> "USDKRW";
            default -> throw new IllegalArgumentException("지원하지 않는 통화쌍: " + currencyPair);
        };
    }

    private String inverseRate(BigDecimal rate) {
        return BigDecimal.ONE.divide(rate, 8, RoundingMode.HALF_EVEN).toPlainString();
    }
}