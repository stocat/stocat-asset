package com.stocat.asset.scraper.crypto.util;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Slf4j
public class TradeParsingUtil {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private TradeParsingUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static BigDecimal readBigDecimal(JsonNode node, String fieldName) {
        JsonNode field = node.path(fieldName);
        if (field.isMissingNode() || field.isNull()) {
            return BigDecimal.ZERO;
        }
        if (field.isNumber()) {
            return field.decimalValue();
        }
        String text = field.asText();
        if (text == null || text.isBlank()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException ex) {
            log.warn("잘못된 숫자 형식 감지: field={}, value={}", fieldName, text);
            return BigDecimal.ZERO;
        }
    }

    public static LocalDateTime toOccurredAt(JsonNode node) {
        return toOccurredAt(node, KST);
    }

    public static LocalDateTime toOccurredAt(JsonNode node, ZoneId zone) {
        long millis = node.path("ttms").asLong(-1L);
        if (millis <= 0) {
            millis = node.path("tms").asLong(System.currentTimeMillis());
        }
        return Instant.ofEpochMilli(millis)
                .atZone(zone)
                .toLocalDateTime();
    }
}
