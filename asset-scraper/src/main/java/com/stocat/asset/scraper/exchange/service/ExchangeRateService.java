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

/**
 * 수집된 환율 데이터를 Redis 채널로 발행(Publish)하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeRateService {

    private final ReactiveStringRedisTemplate redisTemplate;
    private final ObjectMapper mapper;

    /**
     * 환율 데이터를 JSON으로 직렬화하여 Redis 채널에 발행합니다.
     *
     * @param dto 퍼블리시할 환율 정보 DTO
     * @return 퍼블리시 후 구독자 수 반환 Mono
     */
    public Mono<Long> publishRate(ExchangeRateInfo dto) {
        return Mono.fromCallable(() -> mapper.writeValueAsString(dto))
                .flatMap(json -> {
                    log.debug("Redis 환율 차이 퍼블리시 준비 - channel={}, payload={}", ExchangeRateKeys.EXCHANGE_RATES, json);
                    return redisTemplate.convertAndSend(ExchangeRateKeys.EXCHANGE_RATES, json);
                })
                .doOnSuccess(count -> log.debug("Redis 환율 퍼블리시 완료 - subscriberCount={}", count))
                .doOnError(err -> log.error("Redis 환율 퍼블리시 실패", err));
    }
}
