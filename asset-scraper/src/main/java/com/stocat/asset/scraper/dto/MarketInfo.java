package com.stocat.asset.scraper.dto;

import com.stocat.asset.mysql.domain.asset.domain.AssetsCategory;

/**
 * 종목의 코드·한글명·영문명·로고URL·자산카테고리를 담는 공통 DTO입니다.
 */
public record MarketInfo(
        String code,
        String koreanName,
        String englishName,
        String logoUrl,
        AssetsCategory category
) {
    public static MarketInfo createCrypto(String code, String koreanName, String englishName) {
        return new MarketInfo(code, koreanName, englishName, null, AssetsCategory.CRYPTO);
    }

    public static MarketInfo createKrStock(String code, String koreanName, String englishName, String logoUrl) {
        return new MarketInfo(code, koreanName, englishName, logoUrl, AssetsCategory.KOR_STOCK);
    }

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