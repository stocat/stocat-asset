package com.stocat.asset.scraper.kr_stock.service;

import com.stocat.asset.scraper.dto.MarketInfo;
import java.util.Set;
import reactor.core.publisher.Mono;

/**
 * 한국 주식 시장의 종목 정보를 제공하는 인터페이스입니다.
 */
public interface StockMarketProvider {
    /**
     * 수집 대상이 되는 주식 종목들을 반환합니다.
     *
     * @param limit 반환할 종목의 최대 개수
     * @return 종목 정보(MarketInfo)가 담긴 Set
     */
    Mono<Set<MarketInfo>> getTargetStocks(int limit);
}