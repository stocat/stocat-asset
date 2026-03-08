package com.stocat.asset.websocket.api.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.ReactiveSubscription;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@Slf4j
@RequiredArgsConstructor
public class RedisSubscriberService {
    private final ReactiveRedisMessageListenerContainer redisContainer;
    private final ChannelTopic cryptoTradesTopic;
    private final ChannelTopic exchangeRatesTopic;
    private final ChannelTopic krStockTradesTopic;

    /**
     * "stock:trades" 채널 메시지를 실시간으로 스트리밍합니다.
     */
    public Flux<String> subscribeKrStockTrades() {
        return redisContainer.receive(krStockTradesTopic)
                .doOnSubscribe(sub -> log.debug("[KR_STOCK] Redis 토픽 {} 구독 시작", krStockTradesTopic.getTopic()))
                .map(ReactiveSubscription.Message::getMessage)
                .doOnCancel(() -> log.debug("[KR_STOCK] Redis 토픽 {} 구독 종료", krStockTradesTopic.getTopic()))
                .doOnError(e -> log.error("[KR_STOCK] 주식 구독 오류", e));
    }

    /**
     * "crypto:trades" 채널 메시지를 실시간으로 스트리밍합니다.
     */
    public Flux<String> subscribeTrades() {
        return redisContainer.receive(cryptoTradesTopic)
                .doOnSubscribe(sub -> log.debug("Redis 토픽 {} 구독 시작", cryptoTradesTopic.getTopic()))
                .map(ReactiveSubscription.Message::getMessage)
                .doOnCancel(() -> log.debug("Redis 토픽 {} 구독 종료", cryptoTradesTopic.getTopic()))
                .doOnError(e -> log.error("Redis 구독 오류", e));
    }

    /**
     * "exchange:rates" 채널 메시지를 실시간으로 스트리밍합니다.
     */
    public Flux<String> subscribeExchangeRates() {
        return redisContainer.receive(exchangeRatesTopic)
                .doOnSubscribe(sub -> log.debug("Redis 토픽 {} 구독 시작", exchangeRatesTopic.getTopic()))
                .map(ReactiveSubscription.Message::getMessage)
                .doOnCancel(() -> log.debug("Redis 토픽 {} 구독 종료", exchangeRatesTopic.getTopic()))
                .doOnError(e -> log.error("Redis 환율 구독 오류", e));
    }
}
