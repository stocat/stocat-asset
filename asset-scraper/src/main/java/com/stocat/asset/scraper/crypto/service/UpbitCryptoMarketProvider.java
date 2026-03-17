package com.stocat.asset.scraper.crypto.service;

import com.stocat.asset.scraper.crypto.dto.response.MarketEventDetailResponse;
import com.stocat.asset.scraper.dto.MarketInfo;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Upbit REST API 를 호출하여 - 전체 종목 중 거래량 급등 이벤트(trading volume soaring) 종목을 필터링하고, - 상위 N개만 MarketInfo 형태로 반환합니다.
 */
@Service
@RequiredArgsConstructor
public class UpbitCryptoMarketProvider {
    private static final String TRADING_VOLUME_SOARING = "TRADING_VOLUME_SOARING";
    private final WebClient upbitWebClient;

    /**
     * Upbit API를 통해 거래량 급등(TRADING_VOLUME_SOARING) 이벤트가 발생한 KRW 마켓 종목을 조회합니다.
     *
     * @param n 반환할 종목의 최대 개수
     * @return 조회된 종목 정보(MarketInfo)가 담긴 Set을 포함하는 Mono
     */
    public Mono<Set<MarketInfo>> getTargetCrypto(int n) {
        return upbitWebClient.get()
                .uri(uri -> uri.path("/v1/market/all")
                        .queryParam("isDetails", "true")
                        .build())
                .header(HttpHeaders.ACCEPT, "application/json")
                .retrieve()
                .bodyToFlux(MarketEventDetailResponse.class)
                .filter(this::isTradingVolumeSoaring)
                .filter(detail -> detail.market().startsWith("KRW-"))
                .take(n)
                .map(detail -> MarketInfo.createCrypto(detail.market(), detail.koreanName(), detail.englishName()))
                .collect(Collectors.toSet());
    }

    private boolean isTradingVolumeSoaring(MarketEventDetailResponse detail) {
        return detail.marketEvent() != null &&
                detail.marketEvent().caution() != null &&
                Boolean.TRUE.equals(
                        detail.marketEvent().caution().get(TRADING_VOLUME_SOARING)
                );
    }
}
