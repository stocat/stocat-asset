package com.stocat.asset.scraper.exchange.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stocat.asset.redis.constants.ExchangeRateKeys;
import com.stocat.asset.scraper.exchange.dto.ExchangeRateInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 수집된 환율 데이터를 Redis에 저장하고 채널로 발행(Publish)하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeRateService {

    private final ReactiveStringRedisTemplate redisTemplate;
    private final ObjectMapper mapper;

    /**
     * 환율 데이터를 Redis String에 저장(스냅샷)하고 Pub/Sub 채널에 발행합니다.
     *
     * @param dto 퍼블리시할 환율 정보 DTO
     * @return 퍼블리시 후 구독자 수 반환 Mono
     */
    public Mono<Long> publishRate(ExchangeRateInfo dto) {
        return Mono.fromCallable(() -> mapper.writeValueAsString(dto))
                .flatMap(json -> saveSnapshot(dto).and(saveInverseSnapshot(dto)).then(publish(json)))
                .doOnError(err -> log.error("Redis 환율 처리 실패", err));
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
        String value = BigDecimal.ONE.divide(dto.exchangeRate(), 8, RoundingMode.HALF_EVEN).toPlainString();
        return redisTemplate.opsForValue().set(key, value)
                .doOnSuccess(ok -> log.debug("Redis 역방향 환율 스냅샷 저장 - key={}, value={}", key, value));
    }

    // "USDKRW" → "KRWUSD" (ISO 4217 통화 코드는 3자리)
    private String invertPair(String currencyPair) {
        return currencyPair.substring(3) + currencyPair.substring(0, 3);
    }

    private Mono<Long> publish(String json) {
        return redisTemplate.convertAndSend(ExchangeRateKeys.EXCHANGE_RATES, json)
                .doOnSuccess(count -> log.debug("Redis 환율 퍼블리시 완료 - subscriberCount={}", count));
    }
}
