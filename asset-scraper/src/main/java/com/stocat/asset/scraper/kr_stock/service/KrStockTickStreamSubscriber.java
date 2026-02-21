package com.stocat.asset.scraper.kr_stock.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stocat.asset.scraper.kr_stock.config.KrStockProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class KrStockTickStreamSubscriber {

    private static final String APPROVAL_ENDPOINT = "/oauth2/Approval";
    private static final String TR_ID_TICK = "H0STCNT0";

    private final KrStockRedisPublisher publisher;
    private final KrStockSubscriptionService subscriptionService;
    private final ReactorNettyWebSocketClient webSocketClient;
    private final WebClient.Builder webClientBuilder;
    private final KrStockProperties properties;
    private final ObjectMapper mapper;

    private WebClient authClient;

    @PostConstruct
    public void start() {
        if (!properties.isEnabled()) {
            log.info("KR 주식 실시간 구독 비활성화 - kr-stock.enabled=false");
            return;
        }
        if (!hasRequiredConfig()) {
            log.warn("KR 주식 실시간 구독 스킵 - KIS 설정(client/websocket/auth) 확인 필요");
            return;
        }

        var kis = properties.getKis();
        this.authClient = webClientBuilder.clone()
                .baseUrl(kis.getAuthBaseUrl())
                .build();

        subscriptionService.codeFlux()
                .map(symbols -> symbols.stream().limit(properties.getSubscribeLimit()).toList())
                .distinctUntilChanged()
                .switchMap(this::subscribeSymbols)
                .doOnSubscribe(sub -> log.info("KR 주식 구독 코드 flux 구독 시작"))
                .doOnError(error -> log.error("KR 주식 실시간 구독 파이프라인 오류", error))
                .subscribe();
    }

    private boolean hasRequiredConfig() {
        var kis = properties.getKis();
        return StringUtils.hasText(kis.getClientId())
                && StringUtils.hasText(kis.getClientSecret())
                && StringUtils.hasText(kis.getWebsocketUrl())
                && StringUtils.hasText(kis.getAuthBaseUrl());
    }

    private Mono<Void> subscribeSymbols(List<String> symbols) {
        if (symbols.isEmpty()) {
            log.warn("KR 주식 실시간 구독 스킵 - Redis에 구독 코드가 없습니다.");
            return Mono.empty();
        }

        log.info("KR 주식 실시간 구독 시도 - symbols={}", symbols);
        return requestApprovalKey()
                .doOnNext(key -> log.info("KIS approval key 수신 - codes={} keyLength={}", symbols.size(), key.length()))
                .flatMap(key -> streamAndProcess(key, symbols));
    }

    private Mono<String> requestApprovalKey() {
        var kis = properties.getKis();
        return authClient.post()
                .uri(APPROVAL_ENDPOINT)
                .bodyValue(new ApprovalRequest("client_credentials", kis.getClientId(), kis.getClientSecret()))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(node -> node.path("approval_key").asText());
    }

    private Mono<Void> streamAndProcess(String approvalKey, List<String> symbols) {
        Set<String> targetSymbols = Set.copyOf(symbols);
        return webSocketClient.execute(
                URI.create(properties.getKis().getWebsocketUrl()),
                session -> {
                    Flux<WebSocketMessage> sendFlux = Flux.fromIterable(symbols)
                            .map(symbol -> serializePayload(toSubscribePayload(approvalKey, TR_ID_TICK, symbol), symbol))
                            .map(session::textMessage);

                    return session.send(sendFlux)
                            .thenMany(
                                    session.receive()
                                            .map(WebSocketMessage::getPayloadAsText)
                                            .flatMap(payload -> processAndPublish(payload, targetSymbols))
                            )
                            .then();
                }
        ).doOnSubscribe(sub -> log.info("KIS WebSocket 연결 시도 - symbols={}", symbols))
                .doOnError(err -> log.error("KIS WebSocket 연결 오류", err));
    }

    private String serializePayload(Map<String, Object> payload, String symbol) {
        try {
            return mapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize subscription payload for symbol " + symbol, e);
        }
    }

    private Map<String, Object> toSubscribePayload(String approvalKey, String trId, String symbol) {
        Map<String, Object> header = new HashMap<>();
        header.put("approval_key", approvalKey);
        header.put("custtype", "P");
        header.put("tr_type", "1");
        header.put("content-type", "utf-8");

        Map<String, Object> input = new HashMap<>();
        input.put("tr_id", trId);
        input.put("tr_key", symbol);

        Map<String, Object> body = Collections.singletonMap("input", input);
        Map<String, Object> request = new HashMap<>();
        request.put("header", header);
        request.put("body", body);
        return request;
    }

    private Mono<Void> processAndPublish(String payload, Set<String> symbols) {
        try {
            JsonNode node = mapper.readTree(payload);
            String code = node.at("/body/output/stock_code").asText();
            if (symbols.contains(code)) {
                BigDecimal price = new BigDecimal(node.at("/body/output/price").asText());
                return publisher.publishTrade(code, price);
            }
        } catch (Exception e) {
            log.warn("KR 주식 체결 데이터 파싱 실패 payload={}", payload, e);
        }
        return Mono.empty();
    }

    private record ApprovalRequest(String grant_type, String appkey, String secretkey) {
    }
}
