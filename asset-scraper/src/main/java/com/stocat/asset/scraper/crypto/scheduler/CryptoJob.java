package com.stocat.asset.scraper.crypto.scheduler;

import com.stocat.asset.mysql.domain.asset.domain.AssetsCategory;
import com.stocat.asset.redis.constants.CryptoKeys;
import com.stocat.asset.scraper.crypto.config.UpbitApiProperties;
import com.stocat.asset.scraper.crypto.service.UpbitCryptoMarketProvider;
import com.stocat.asset.scraper.service.SubscriptionCodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CryptoJob {

    private final SubscriptionCodeService subscriptionCodeService;
    private final UpbitCryptoMarketProvider upbitCryptoMarketProvider;
    private final UpbitApiProperties upbitApiProperties;


    /**
     * 매일 00:00에 스케줄 1) Upbit에서 Top N개의 랜덤 종목 조회 2) 기존 hotKey 리스트 삭제 후 PUSH 3) 같은 5개 코드를 subscribeKey 에도 PUSH →
     * SubscriptionCodeService가 자동 재구독
     */
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void refreshCryptoMarkets() {
        upbitCryptoMarketProvider.getTargetCrypto(upbitApiProperties.getTopLimit())
                .doOnError(error -> log.error("코인 종목 선정 중 오류 발생", error))
                .flatMap(dailyCodesSet ->
                        subscriptionCodeService.refreshHotAndSubscribeCodes(
                                        dailyCodesSet,
                                        CryptoKeys.CRYPTO_HOT_CODES,
                                        CryptoKeys.CRYPTO_SUBSCRIBE_CODES,
                                        AssetsCategory.CRYPTO
                                )
                                .then(subscriptionCodeService.reloadCodes(
                                        CryptoKeys.CRYPTO_SUBSCRIBE_CODES,
                                        AssetsCategory.CRYPTO
                                ))
                )
                .doOnError(error -> log.error("코인 종목 갱신 실패", error))
                .subscribe();
    }

}
