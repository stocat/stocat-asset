package com.stocat.asset.scraper.exchange.api;

import com.stocat.asset.scraper.exchange.dto.ExchangeRateInfo;
import com.stocat.asset.scraper.exchange.service.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * 외부(예: 파이썬 크롤러)로부터 환율 데이터를 수신하는 컨트롤러
 */
@RestController
@RequestMapping("/api/v1/exchange-rates")
@RequiredArgsConstructor
public class ExchangeRateController {

    private final ExchangeRateService publisher;

    /**
     * 크롤러에서 수집한 실시간 환율 정보를 수신하여 Redis로 퍼블리시합니다.
     *
     * @param info 수신된 환율 정보 DTO
     * @return 처리가 완료됨을 알리는 Mono<Void>
     */
    @PostMapping
    public Mono<Void> receiveExchangeRate(@RequestBody ExchangeRateInfo info) {
        return publisher.publishRate(info).then();
    }
}
