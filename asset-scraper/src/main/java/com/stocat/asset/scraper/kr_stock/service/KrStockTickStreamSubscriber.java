package com.stocat.asset.scraper.kr_stock.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stocat.asset.mysql.domain.asset.domain.Currency;
import com.stocat.asset.scraper.kr_stock.config.KrStockProperties;
import com.stocat.asset.scraper.messaging.event.TradeInfo;
import com.stocat.asset.scraper.messaging.event.TradeSide;
import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 한국투자증권(KIS) 실시간 주식 호가(H0STASP0) 웹소켓 구독 서비스.
 * <p>
 * 설정된 종목들의 실시간 호가 데이터를 수신하여 {@link TradeInfo} 형태로 변환 후 Redis로 발행합니다. 매도 1호가와 매수 1호가 정보만을 추출하여 처리합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KrStockTickStreamSubscriber {

    private static final String APPROVAL_ENDPOINT = "/oauth2/Approval";
    /**
     * 실시간 주식 호가 TR ID
     */
    private static final String TR_ID_TICK = "H0STASP0";

    // 데이터 인덱스 상수 정의 (API 문서 기준)
    /**
     * 종목 코드 인덱스
     */
    private static final int IDX_CODE = 0;
    /**
     * 매도 1호가 인덱스
     */
    private static final int IDX_ASK_PRICE_1 = 3;
    /**
     * 매수 1호가 인덱스
     */
    private static final int IDX_BID_PRICE_1 = 13;
    /**
     * 매도 1호가 잔량 인덱스
     */
    private static final int IDX_ASK_SIZE_1 = 23;
    /**
     * 매수 1호가 잔량 인덱스
     */
    private static final int IDX_BID_SIZE_1 = 33;

    private final KrStockRedisPublisher publisher;
    private final KrStockSubscriptionService subscriptionService;
    private final ReactorNettyWebSocketClient webSocketClient;
    private final WebClient.Builder webClientBuilder;
    private final KrStockProperties properties;
    private final ObjectMapper mapper;

    private WebClient authClient;

    /**
     * 서비스 시작 시 초기화 및 구독 프로세스를 시작합니다.
     * <p>
     * 1. 기능 활성화 여부 및 필수 설정 확인 2. 인증용 WebClient 초기화 3. 구독 대상 종목 변경 감지 및 웹소켓 연결 시작
     */
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
                .switchMap(symbols -> this.subscribeSymbols(symbols)
                        .onErrorResume(error -> {
                            // 웹소켓 연결 실패 시 로그만 남기고 빈 스트림으로 대체
                            log.error("KR 주식 구독 실패. 다음 종목 업데이트 시 재시도합니다. symbols={}", symbols, error);
                            return Mono.empty(); // 실패한 stream만 종료
                        })
                )
                .doOnSubscribe(sub -> log.info("KR 주식 구독 코드 flux 구독 시작"))
                .onErrorResume(error -> {
                    log.error("KR 주식 구독 외부 파이프라인 오류", error);
                    return Mono.empty();
                })
                .subscribe();
    }

    private boolean hasRequiredConfig() {
        var kis = properties.getKis();
        return StringUtils.hasText(kis.getClientId())
                && StringUtils.hasText(kis.getClientSecret())
                && StringUtils.hasText(kis.getWebsocketUrl())
                && StringUtils.hasText(kis.getAuthBaseUrl());
    }

    /**
     * 주어진 종목 리스트에 대해 웹소켓 구독을 수행합니다.
     * <p>
     * 1. 웹소켓 접속키(Approval Key) 발급 요청 2. 발급받은 키로 웹소켓 연결 및 데이터 스트리밍 시작
     *
     * @param symbols 구독할 종목 코드 리스트
     * @return 완료 시그널 Mono
     */
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

    /**
     * KIS API를 통해 웹소켓 접속키를 발급받습니다.
     *
     * @return 접속키(approval_key) Mono
     */
    private Mono<String> requestApprovalKey() {
        var kis = properties.getKis();
        return authClient.post()
                .uri(APPROVAL_ENDPOINT)
                .bodyValue(new ApprovalRequest("client_credentials", kis.getClientId(), kis.getClientSecret()))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(node -> node.path("approval_key").asText());
    }

    /**
     * 웹소켓을 연결하고 데이터를 수신하여 처리합니다.
     *
     * @param approvalKey 웹소켓 접속키
     * @param symbols     구독할 종목 코드 리스트
     * @return 완료 시그널 Mono
     */
    private Mono<Void> streamAndProcess(String approvalKey, List<String> symbols) {
        Set<String> targetSymbols = Set.copyOf(symbols);
        return webSocketClient.execute(
                        URI.create(properties.getKis().getWebsocketUrl()),
                        session -> {
                            Flux<WebSocketMessage> sendFlux = Flux.fromIterable(symbols)
                                    .map(symbol -> serializePayload(toSubscribePayload(approvalKey, TR_ID_TICK, symbol),
                                            symbol))
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

    private String serializePayload(Object payload, String symbol) {
        try {
            return mapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize subscription payload for symbol " + symbol, e);
        }
    }

    /**
     * 웹소켓 구독 요청을 위한 페이로드 객체를 생성합니다.
     *
     * @param approvalKey 웹소켓 접속키
     * @param trId        거래 ID (예: H0STASP0)
     * @param symbol      종목 코드
     * @return 구독 요청 객체 (SubscribeRequest)
     */
    private SubscribeRequest toSubscribePayload(String approvalKey, String trId, String symbol) {
        SubscribeHeader header = new SubscribeHeader(approvalKey, "P", "1", "utf-8");
        SubscribeInput input = new SubscribeInput(trId, symbol);
        SubscribeBody body = new SubscribeBody(input);
        return new SubscribeRequest(header, body);
    }

    /**
     * 수신된 웹소켓 메시지를 파싱하고 Redis로 발행합니다.
     * <p>
     * 메시지 형식: 0|H0STASP0|001|종목코드^영업시간^...^매도1호가^...
     *
     * @param payload 수신된 웹소켓 메시지 (Text)
     * @param symbols 구독 중인 종목 코드 Set (필터링용)
     * @return 완료 시그널 Mono
     */
    private Mono<Void> processAndPublish(String payload, Set<String> symbols) {
        try {
            if (payload.trim().startsWith("{")) {
                // 구독 성공 메시지 등은 JSON으로 오지만, 호가 데이터 처리에는 필요 없으므로 무시
                return Mono.empty();
            }

            // 실시간 데이터는 '|'와 '^'로 구분됨
            String[] parts = payload.split("\\|");
            if (parts.length < 4) {
                return Mono.empty();
            }

            String trId = parts[1];
            if (!TR_ID_TICK.equals(trId)) {
                return Mono.empty();
            }

            String dataPart = parts[3];
            String[] data = dataPart.split("\\^");

            String code = data[IDX_CODE];

            if (!symbols.contains(code)) {
                return Mono.empty();
            }

            return Flux.fromIterable(parseTradeInfo(code, data))
                    .flatMap(publisher::publishTrade)
                    .then();

        } catch (Exception e) {
            log.warn("KR 주식 호가 데이터 파싱 실패 payload={}", payload, e);
        }
        return Mono.empty();
    }

    private List<TradeInfo> parseTradeInfo(String code, String[] data) {
        // 매도 1호가 정보 발행
        BigDecimal askPrice1 = new BigDecimal(data[IDX_ASK_PRICE_1]);
        BigDecimal askSize1 = new BigDecimal(data[IDX_ASK_SIZE_1]);
        TradeInfo askTrade = new TradeInfo(
                code,
                TradeSide.SELL,
                askSize1,
                askPrice1,
                Currency.KRW,
                BigDecimal.ZERO,
                Currency.KRW,
                LocalDateTime.now() // 호가 데이터에는 타임스탬프가 없으므로 현재 시간 사용
        );

        // 매수 1호가 정보 발행
        BigDecimal bidPrice1 = new BigDecimal(data[IDX_BID_PRICE_1]);
        BigDecimal bidSize1 = new BigDecimal(data[IDX_BID_SIZE_1]);
        TradeInfo bidTrade = new TradeInfo(
                code,
                TradeSide.BUY,
                bidSize1,
                bidPrice1,
                Currency.KRW,
                BigDecimal.ZERO,
                Currency.KRW,
                LocalDateTime.now()
        );

        return List.of(askTrade, bidTrade);
    }

    private record ApprovalRequest(
            @JsonProperty("grant_type") String grantType,
            @JsonProperty("appkey") String appKey,
            @JsonProperty("secretkey") String secretKey
    ) {
    }

    private record SubscribeRequest(
            @JsonProperty("header") SubscribeHeader header,
            @JsonProperty("body") SubscribeBody body
    ) {
    }

    private record SubscribeHeader(
            @JsonProperty("approval_key") String approvalKey,
            @JsonProperty("custtype") String custType,
            @JsonProperty("tr_type") String trType,
            @JsonProperty("content-type") String contentType
    ) {
    }

    private record SubscribeBody(
            @JsonProperty("input") SubscribeInput input
    ) {
    }

    private record SubscribeInput(
            @JsonProperty("tr_id") String trId,
            @JsonProperty("tr_key") String trKey
    ) {
    }
}
