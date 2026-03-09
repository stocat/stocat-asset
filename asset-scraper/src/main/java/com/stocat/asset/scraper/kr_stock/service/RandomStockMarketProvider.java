package com.stocat.asset.scraper.kr_stock.service;

import com.stocat.asset.scraper.dto.MarketInfo;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * 테스트 및 개발용 랜덤 우량주 제공자입니다.
 * <p>
 * 미리 정의된 우량주 목록에서 요청된 개수만큼 무작위로 선택하여 반환합니다.
 * </p>
 */
@Service
public class RandomStockMarketProvider implements StockMarketProvider {

    // 임의의 우량주 목록
    private static final List<MarketInfo> POOL = List.of(
            MarketInfo.createKrStock("005930", "삼성전자", "Samsung Electronics", "https://example.com/samsung.png"),
            MarketInfo.createKrStock("000660", "SK하이닉스", "SK Hynix", "https://example.com/skhynix.png"),
            MarketInfo.createKrStock("373220", "LG에너지솔루션", "LG Energy Solution", "https://example.com/lges.png"),
            MarketInfo.createKrStock("207940", "삼성바이오로직스", "Samsung Biologics", "https://example.com/samba.png"),
            MarketInfo.createKrStock("005380", "현대차", "Hyundai Motor", "https://example.com/hyundai.png"),
            MarketInfo.createKrStock("000270", "기아", "Kia", "https://example.com/kia.png"),
            MarketInfo.createKrStock("068270", "셀트리온", "Celltrion", "https://example.com/celltrion.png"),
            MarketInfo.createKrStock("105560", "KB금융", "KB Financial Group", "https://example.com/kb.png"),
            MarketInfo.createKrStock("005490", "POSCO홀딩스", "POSCO Holdings", "https://example.com/posco.png"),
            MarketInfo.createKrStock("035420", "NAVER", "NAVER", "https://example.com/naver.png")
    );

    /**
     * 미리 정의된 우량주 목록 중 무작위로 N개를 선택하여 반환합니다.
     *
     * @param limit 반환할 종목 개수
     * @return 선택된 종목 코드들의 Set
     */
    @Override
    public Set<MarketInfo> getTargetStocks(int limit) {
        List<MarketInfo> shuffled = new ArrayList<>(POOL);
        Collections.shuffle(shuffled);

        return shuffled.stream()
                .limit(limit)
                .collect(Collectors.toSet());
    }
}