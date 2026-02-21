package com.stocat.asset.scraper.crypto.dto;

/**
 * REST로 받은 시장 정보 중,
 * 종목의 코드·한글명·영문명을 담는 DTO입니다.
 */
public record MarketInfo(
        String code,
        String koreanName,
        String englishName
) {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MarketInfo other)) return false;
        return code != null && code.equals(other.code);
    }

    @Override
    public int hashCode() {
        return code != null ? code.hashCode() : 0;
    }
}