package com.stocat.asset.scraper.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stocat.asset.mysql.domain.asset.domain.AssetsCategory;
import com.stocat.asset.mysql.domain.asset.domain.AssetsEntity;
import com.stocat.asset.mysql.domain.asset.domain.Currency;
import com.stocat.asset.mysql.domain.asset.repository.AssetsRepository;
import com.stocat.asset.scraper.dto.MarketInfo;
import com.stocat.asset.scraper.messaging.event.TradeInfo;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

/**
 * Redis의 구독 코드를 관리하고 변경 사항을 전파하는 공통 서비스입니다. Crypto와 Stock 등 자산군별로 Bean을 생성하여 사용합니다.
 */
@Slf4j
@RequiredArgsConstructor
public class SubscriptionCodeService {

    private final ReactiveStringRedisTemplate redisTemplate;
    private final AssetsRepository assetsRepository;
    private final ObjectMapper mapper;

    // 자산군별 설정 필드
    private final String subscribeKey;
    private final String hotKey;
    private final String tradeChannel;
    private final AssetsCategory assetsCategory;

    private final Sinks.Many<List<String>> sink = Sinks.many().replay().latest();

    /**
     * Redis에서 리스트를 다시 읽어 와서 구독 Flux에 푸시합니다.
     */
    public Mono<Void> reloadCodes() {
        return redisTemplate.opsForSet()
                .members(subscribeKey)
                .collectList()
                .filter(list -> !list.isEmpty())
                .doOnNext(list -> log.debug("Redis 구독 코드 로드 완료 ({}): {}", assetsCategory, list))
                .doOnNext(sink::tryEmitNext)
                .doOnSubscribe(sub -> log.debug("Redis 구독 코드 로드 시도 ({})", assetsCategory))
                .doOnError(err -> log.error("Redis 구독 코드 로드 실패 ({})", assetsCategory, err))
                .then();
    }

    /**
     * 구독 코드가 로드될 때마다 리스트를 방출하는 Flux.
     */
    public Flux<List<String>> codeFlux() {
        return sink.asFlux();
    }

    /**
     * DB에 자산 정보를 저장하고 Redis 키를 갱신합니다.
     */
    public Mono<Void> refreshHotAndSubscribeCodes(Set<MarketInfo> targetSymbols) {
        // 1. DB 저장 작업 (블로킹 작업이므로 별도 스레드에서 수행)
        Mono<Void> dbTask = Mono.fromRunnable(() -> {
            List<AssetsEntity> newAssets = targetSymbols.stream()
                    .map(info -> AssetsEntity.create(
                            info.code(),
                            info.koreanName(),
                            info.englishName(),
                            assetsCategory,
                            Currency.KRW)
                    )
                    .toList();
            assetsRepository.saveAll(newAssets);
            log.debug("DB 갱신 완료 ({}) - 저장된 자산 수: {}", assetsCategory, newAssets.size());
        }).subscribeOn(Schedulers.boundedElastic()).then();

        // 2. Redis 저장 작업 (논블로킹 작업이므로 즉시 실행)
        Mono<Void> redisTask = Mono.defer(() -> {
            String[] codes = targetSymbols.stream()
                    .map(MarketInfo::code)
                    .toArray(String[]::new);
            return updateRedisKeys(codes);
        });

        // 3. 두 작업을 동시에 실행하고, 둘 다 끝나면 종료
        return Mono.when(dbTask, redisTask);
    }

    private Mono<Void> updateRedisKeys(String[] codes) {
        return redisTemplate.delete(hotKey)
                .then(redisTemplate.opsForSet().add(hotKey, codes))
                .then(redisTemplate.opsForSet().add(subscribeKey, codes))
                .doOnSubscribe(sub -> log.debug("Redis 핫/구독 코드 갱신 시작 ({}) - count={}", assetsCategory, codes.length))
                .doOnSuccess(count -> log.debug("Redis 핫/구독 코드 갱신 완료 ({}) - count={}", assetsCategory, count))
                .doOnError(err -> log.error("Redis 핫/구독 코드 갱신 실패 ({})", assetsCategory, err))
                .then();
    }

    /**
     * TradeDto를 JSON으로 직렬화하여 Redis 채널에 발행합니다.
     */
    public Mono<Long> publishTrades(TradeInfo dto) {
        try {
            String json = mapper.writeValueAsString(dto);
            log.debug("Redis 퍼블리시 준비 - channel={}, payload={}", tradeChannel, json);
            return redisTemplate.convertAndSend(tradeChannel, json)
                    .doOnSuccess(count -> log.debug("Redis 퍼블리시 완료 - subscriberCount={} (구독자가 없을 경우 0)", count));
        } catch (JsonProcessingException e) {
            return Mono.error(e);
        }
    }
}