package com.stocat.asset.scraper.crypto.service;

import com.stocat.asset.scraper.crypto.dto.MarketInfo;
import com.stocat.asset.scraper.crypto.dto.response.MarketEventDetailResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Upbit REST API 를 호출하여
 * - 전체 종목 중 거래량 급등 이벤트(trading volume soaring) 종목을 필터링하고,
 * - 상위 N개만 MarketInfo 형태로 반환합니다.
 */
@Service
@RequiredArgsConstructor
public class UpbitCryptoMarketService {
    private static final String TRADING_VOLUME_SOARING = "TRADING_VOLUME_SOARING";
    private final WebClient upbitWebClient;

    public Set<MarketInfo> getTopKrwTradeCrypto(int n) {
        return Objects.requireNonNull(upbitWebClient.get()
                        .uri(uri -> uri.path("/v1/market/all")
                                .queryParam("isDetails", "true")
                                .build())
                        .header(HttpHeaders.ACCEPT, "application/json")
                        .retrieve()
                        .bodyToFlux(MarketEventDetailResponse.class)
                        .collectList()
                        .block())
                .stream()
                .filter(UpbitCryptoMarketService::isTradingVolumeSoaring
                )
                .filter(detail -> detail.market().startsWith("KRW-"))
                .limit(n)
                .map(detail -> new MarketInfo(detail.market(), detail.koreanName(), detail.englishName()))
                .collect(Collectors.toSet());
    }

    private static boolean isTradingVolumeSoaring(MarketEventDetailResponse detail) {
        return detail.marketEvent() != null &&
                detail.marketEvent().caution() != null &&
                Boolean.TRUE.equals(
                        detail.marketEvent().caution().get(TRADING_VOLUME_SOARING)
                );
    }

    /**
     * SOARING 이벤트 종목 중 랜덤 샘플 n 개를 반환합니다.
     * 만약 SOARING 종목이 n개 미만이라면, 전체 코드에서 채워 넣습니다.
     * *** 경고 기준으로는 거래량이 없는 코인이 많아서 사용하지 않습니다. ***
     */
    @Deprecated
    public Set<MarketInfo> getDailyCrypto(int n) {
        return upbitWebClient.get()
                .uri(uri -> uri.path("/v1/market/all")
                        .queryParam("isDetails", "true")
                        .build())
                .header(HttpHeaders.ACCEPT, "application/json")
                .retrieve()
                .bodyToFlux(MarketEventDetailResponse.class)
                .collectList()
                .map(list -> pickOrFill(list, n))
                .map(this::toMarketInfo)
                .block();
    }

    @Deprecated
    private Set<MarketInfo> toMarketInfo(List<MarketEventDetailResponse> response) {
        return response.stream().map(detail -> new MarketInfo(detail.market(), detail.koreanName(), detail.englishName())).collect(Collectors.toSet());
    }

    /**
     * 1) allDetails 에서 marketEvent.caution 에 "SOARING" 포함된 market 코드만 뽑아서 셔플
     * 2) allDetails 에서 모든 market 코드만 뽑아서 셔플
     * 3) 두 스트림을 합친 뒤 distinct() -> limit 개수만큼 반환
     */
    @Deprecated
    private List<MarketEventDetailResponse> pickOrFill(Collection<MarketEventDetailResponse> allDetails, int limit) {
        List<MarketEventDetailResponse> marketList = new ArrayList<>(getVolumeSoaring(allDetails));
        Collections.shuffle(marketList);

        if (marketList.size() >= limit) {
            return marketList.stream()
                    .limit(limit)
                    .collect(Collectors.toList());
        }

        int remaining = limit - marketList.size();
        List<MarketEventDetailResponse> otherCaution = getSoaringWithoutVolume(allDetails, marketList);
        Collections.shuffle(otherCaution);

        otherCaution = otherCaution.stream().limit(remaining).toList();
        marketList.addAll(otherCaution);

        if (marketList.size() >= limit) {
            return marketList.stream()
                    .limit(limit)
                    .collect(Collectors.toList());
        }

        remaining = limit - marketList.size();
        List<MarketEventDetailResponse> reamainList = allDetails.stream()
                .filter(list -> !marketList.contains(list))
                .limit(remaining)
                .toList();

        marketList.addAll(reamainList);
        return marketList;
    }

    @Deprecated
    private List<MarketEventDetailResponse> getSoaringWithoutVolume(Collection<MarketEventDetailResponse> allDetails, List<MarketEventDetailResponse> volSoaring) {
        return allDetails.stream()
                .filter(md -> md.marketEvent() != null && md.marketEvent().caution() != null)
                .filter(md -> md.marketEvent().caution().entrySet().stream()
                        .anyMatch(e -> Boolean.TRUE.equals(e.getValue())))
                .filter(md -> !volSoaring.contains(md))
                .distinct()
                .toList();
    }

    @Deprecated
    private List<MarketEventDetailResponse> getVolumeSoaring(Collection<MarketEventDetailResponse> allDetails) {
        return allDetails.stream()
                .filter(md -> md.marketEvent() != null && md.marketEvent().caution() != null)
                .filter(md -> Boolean.TRUE.equals(md.marketEvent().caution().get(TRADING_VOLUME_SOARING)))
                .distinct()
                .toList();
    }

}
