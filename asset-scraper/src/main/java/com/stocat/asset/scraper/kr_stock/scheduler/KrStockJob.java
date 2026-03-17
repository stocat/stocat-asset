package com.stocat.asset.scraper.kr_stock.scheduler;

import com.stocat.asset.mysql.domain.asset.domain.AssetsCategory;
import com.stocat.asset.redis.constants.KrStockKeys;
import com.stocat.asset.scraper.kr_stock.config.KrStockProperties;
import com.stocat.asset.scraper.kr_stock.service.StockMarketProvider;
import com.stocat.asset.mysql.domain.asset.domain.Currency;
import com.stocat.asset.scraper.service.SubscriptionCodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 한국 주식 데이터 수집 및 구독 갱신을 위한 스케줄러 클래스입니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KrStockJob {

    private final SubscriptionCodeService subscriptionCodeService;
    private final StockMarketProvider stockMarketProvider;
    private final KrStockProperties properties;

    /**
     * 매일 00:00에 스케줄 1) 랜덤한 N개의 한국 주식 종목 조회 2) 기존 hotKey 리스트 삭제 후 PUSH 3) 같은 5개 코드를 subscribeKey 에도 PUSH →
     * SubscriptionCodeService가 자동 재구독
     */
    @EventListener(ApplicationReadyEvent.class)
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void refreshStockCodes() {
        stockMarketProvider.getTargetStocks(properties.getSubscribeLimit())
                .doOnError(e -> log.error("한국 주식 종목 선정 중 오류 발생", e))
                .flatMap(dailyCodesSet ->
                        subscriptionCodeService.refreshHotAndSubscribeCodes(
                                        dailyCodesSet,
                                        KrStockKeys.KR_STOCK_HOT_CODES,
                                        KrStockKeys.KR_STOCK_SUBSCRIBE_CODES,
                                        AssetsCategory.KOR_STOCK,
                                        Currency.KRW
                                )
                                .then(subscriptionCodeService.reloadCodes(
                                        KrStockKeys.KR_STOCK_SUBSCRIBE_CODES,
                                        AssetsCategory.KOR_STOCK
                                ))
                                .doOnError(e -> log.error("Redis 구독 코드 갱신 또는 리로드 중 오류 발생", e))
                )
                .doOnError(error -> log.error("한국 주식 구독 코드 갱신 파이프라인 최종 실패", error))
                .subscribe();
    }
}
