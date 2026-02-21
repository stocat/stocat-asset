package com.stocat.asset.scraper.crypto.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * /v1/market/all?isDetails=true 응답 한 건을 매핑합니다.
 */
public record MarketEventDetailResponse(
        @JsonProperty("market") String market,
        @JsonProperty("korean_name") String koreanName,
        @JsonProperty("english_name") String englishName,
        @JsonProperty("market_event") MarketEvent marketEvent
) {
    public record MarketEvent(
            @JsonProperty("warning") boolean warning,
            /**
             * "caution" 필드는 이벤트명→Boolean 구조입니다.
             * 실제 JSON:
             *   "caution": {
             *     "TRADING_VOLUME_SOARING": false,
             *     ...
             *   }
             */
            @JsonProperty("caution") Map<String, Boolean> caution
    ) { }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MarketEventDetailResponse other)) return false;
        return market != null && market.equals(other.market);
    }

    @Override
    public int hashCode() {
        return market != null ? market.hashCode() : 0;
    }
}