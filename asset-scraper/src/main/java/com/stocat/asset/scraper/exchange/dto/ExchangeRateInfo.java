package com.stocat.asset.scraper.exchange.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 환율 정보용 DTO
 */
public record ExchangeRateInfo(
                String currencyPair, // 통화쌍 (예: USDKRW)
                BigDecimal exchangeRate, // 환율
                LocalDateTime timestamp // 기준 시간
) {
}
