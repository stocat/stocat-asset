package com.stocat.asset.scraper.crypto.scheduler;

import com.stocat.asset.scraper.crypto.config.UpbitApiProperties;
import com.stocat.asset.scraper.crypto.service.UpbitCryptoMarketProvider;
import com.stocat.asset.scraper.dto.MarketInfo;
import com.stocat.asset.scraper.service.SubscriptionCodeService;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CryptoJob {

    private final SubscriptionCodeService cryptoSubscriptionService;
    private final UpbitCryptoMarketProvider upbitCryptoMarketProvider;
    private final UpbitApiProperties upbitApiProperties;


    /**
     * 매일 00:00에 스케줄 1) Upbit에서 Top N개의 랜덤 종목 조회 2) 기존 hotKey 리스트 삭제 후 PUSH 3) 같은 5개 코드를 subscribeKey 에도 PUSH →
     * SubscriptionCodeService가 자동 재구독
     */
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void refreshHodCodeAndAddJobSubscribe() {
        Set<MarketInfo> dailyCodesSet = upbitCryptoMarketProvider.getTargetCrypto(upbitApiProperties.getTopLimit());
        cryptoSubscriptionService.refreshHotAndSubscribeCodes(dailyCodesSet)
                .then(cryptoSubscriptionService.reloadCodes())
                .subscribe();
    }

}
