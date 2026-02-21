package com.stocat.asset.scraper.crypto.messaging.event;

import com.stocat.asset.mysql.domain.asset.domain.Currency;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 체결 정보용 DTO
 */
public record TradeInfo(
        String code, // 종목 코드
        TradeSide side, // 매매 구분 (BUY/SELL)
        BigDecimal qty, // 주문 수량
        BigDecimal price, // 체결가
        Currency priceCurrency, // 가격 통화
        BigDecimal feeAmount, // 수수료 금액
        Currency feeCurrency, // 수수료 통화
        LocalDateTime occurredAt // 체결 일시 (KST)
) {
}