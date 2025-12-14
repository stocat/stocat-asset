package com.stocat.asset.scraper.crypto.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stocat.asset.scraper.crypto.config.UpbitApiProperties;
import com.stocat.asset.scraper.crypto.messaging.event.TradeInfo;
import com.stocat.asset.scraper.crypto.messaging.event.TradeSide;
import com.stocat.common.domain.asset.domain.Currency;
import com.stocat.asset.scraper.crypto.util.TradeParsingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;

/**
 * SubscriptionCodeService.codeFlux()에서 전달된 종목 리스트마다
 * Upbit WS에 재구독하고, 수신된 체결 메시지를 Redis "crypto:trades" 채널에 퍼블리시합니다.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UpbitCryptoScrapeService {
    private final ObjectMapper mapper;
    private final ReactorNettyWebSocketClient webSocketClient;
    private final UpbitApiProperties upbitApiProperties;

    /**
     * 주어진 심볼 리스트로 WebSocket에 연결하여
     * TradeInfo 파이프라인을 반환합니다.
     */
    public Flux<TradeInfo> streamTrades(List<String> symbols) {
        String payload = buildSubscribePayload(symbols);

        return Flux.create(sink -> {
            Disposable subscription = webSocketClient.execute(
                            URI.create(upbitApiProperties.getWsUrl()),
                            session -> session
                                    .send(Mono.just(session.textMessage(payload)))
                                    .thenMany(
                                            session.receive()
                                                    .map(WebSocketMessage::getPayloadAsText)
                                                    .doOnSubscribe(sub -> log.debug("Upbit WebSocket 세션 수신 시작"))
                                                    .flatMap(this::parseJson)
                                                    .filter(this::isTrade)
                                                    .map(this::toTradeInfo)
                                                    .doOnNext(trade -> log.debug("Upbit 체결 수신: {}", trade))
                                                    .doOnNext(sink::next)
                                    )
                                    .then()
                    )
                    .doOnSubscribe(sub -> log.debug("Upbit WebSocket 실행 스케줄 등록 - codes={}", symbols))
                    .doOnError(error -> {
                        log.error("Upbit WebSocket 실행 중 오류", error);
                        sink.error(error);
                    })
                    .subscribeOn(Schedulers.boundedElastic())
                    .subscribe();

            sink.onDispose(() -> {
                log.debug("Upbit WebSocket 연결 종료 요청 - codes={}", symbols);
                subscription.dispose();
            });
        });
    }

    /**
     * 구독 페이로드(JSON Array)를 생성합니다.
     */
    private String buildSubscribePayload(List<String> symbols) {
        var ticket = mapper.createObjectNode()
                .put("ticket", "daily-" + System.currentTimeMillis());
        var type = mapper.createObjectNode()
                .put("type", "trade")
                .set("codes", mapper.valueToTree(symbols));
        var format = mapper.createObjectNode()
                .put("format", "SIMPLE");
        return mapper.createArrayNode()
                .add(ticket)
                .add(type)
                .add(format)
                .toString();
    }

    /**
     * 원시 JSON 문자열을 JsonNode로 파싱합니다.
     */
    private Mono<JsonNode> parseJson(String raw) {
        return Mono.fromCallable(() -> mapper.readTree(raw))
                .onErrorResume(sub -> Mono.empty());
    }

    /**
     * SIMPLE 포맷 메시지인지, 그리고 체결(trade) 타입인지 확인합니다.
     */
    private boolean isTrade(JsonNode node) {
        return "trade".equals(node.path("ty").asText());
    }

    /**
     * JsonNode를 TradeInfo로 변환
     */
    private TradeInfo toTradeInfo(JsonNode node) {
        String code = node.path("cd").asText();
        TradeSide side = TradeSide.fromUpbitSide(node.path("ab").asText());
        BigDecimal qty = TradeParsingUtil.readBigDecimal(node, "tv");
        BigDecimal price = TradeParsingUtil.readBigDecimal(node, "tp");
        Currency currency = Currency.fromMarket(code);
        LocalDateTime occurredAt = TradeParsingUtil.toOccurredAt(node);
        return new TradeInfo(
                code,
                side,
                qty,
                price,
                currency,
                BigDecimal.ZERO,
                currency,
                occurredAt
        );
    }
}
