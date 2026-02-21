package com.stocat.asset.scraper.crypto.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * /v1/ticker 응답 한 건을 매핑합니다.
 */
@Deprecated
public record MarketDetailResponse(
        @JsonProperty("market") String market,
        @JsonProperty("trade_date") String tradeDate,
        @JsonProperty("trade_time") String tradeTime,
        @JsonProperty("trade_date_kst") String tradeDateKst,
        @JsonProperty("trade_time_kst") String tradeTimeKst,
        @JsonProperty("trade_timestamp") long tradeTimestamp,
        @JsonProperty("opening_price") double openingPrice,
        @JsonProperty("high_price") double highPrice,
        @JsonProperty("low_price") double lowPrice,
        @JsonProperty("trade_price") double tradePrice,
        @JsonProperty("prev_closing_price") double prevClosingPrice,
        @JsonProperty("change") String change,
        @JsonProperty("change_price") double changePrice,
        @JsonProperty("change_rate") double changeRate,
        @JsonProperty("signed_change_price") double signedChangePrice,
        @JsonProperty("signed_change_rate") double signedChangeRate,
        @JsonProperty("trade_volume") double tradeVolume,
        @JsonProperty("acc_trade_price") double accTradePrice,
        @JsonProperty("acc_trade_price_24h") double accTradePrice24h,
        @JsonProperty("acc_trade_volume") double accTradeVolume,
        @JsonProperty("acc_trade_volume_24h") double accTradeVolume24h,
        @JsonProperty("highest_52_week_price") double highest52WeekPrice,
        @JsonProperty("highest_52_week_date") String highest52WeekDate,
        @JsonProperty("lowest_52_week_price") double lowest52WeekPrice,
        @JsonProperty("lowest_52_week_date") String lowest52WeekDate,
        @JsonProperty("timestamp") long timestamp
) {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MarketDetailResponse that)) return false;
        return market != null && market.equals(that.market);
    }

    @Override
    public int hashCode() {
        return market != null ? market.hashCode() : 0;
    }
}