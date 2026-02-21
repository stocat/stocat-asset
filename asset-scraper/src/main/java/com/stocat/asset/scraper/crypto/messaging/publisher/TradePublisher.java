package com.stocat.asset.scraper.crypto.messaging.publisher;

import com.stocat.asset.scraper.crypto.service.SubscriptionCodeService;
import com.stocat.asset.scraper.crypto.service.UpbitCryptoScrapeService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class TradePublisher {

    private final SubscriptionCodeService subscriptionCodeService;
    private final UpbitCryptoScrapeService upbitCryptoScrapeService;

    @PostConstruct
    public void start() {
        // reloadCodes() 호출로 구독 키 리스트가 갱신될 때마다 websocket 다시 열고, 체결 데이터 redis 채널로 pub 파이프라인
        subscriptionCodeService.codeFlux()
                .doOnSubscribe(sub -> log.debug("체결 퍼블리시 파이프라인 구독 시작"))
                .doOnNext(codes -> log.debug("새 구독 코드 수신: {}", codes))
                .map(codes -> codes.stream().sorted().toList()) // Redis Set은 순서가 없으므로 정렬 후 비교
                .distinctUntilChanged()
                .switchMap(upbitCryptoScrapeService::streamTrades)
                .flatMap(subscriptionCodeService::publishTrades)
                .doOnError(err -> log.error("체결 퍼블리시 파이프라인 오류", err))
                .subscribe(
                        null,
                        err -> System.err.println("WebSocket 오류: " + err.getMessage())
                );
    }

}
