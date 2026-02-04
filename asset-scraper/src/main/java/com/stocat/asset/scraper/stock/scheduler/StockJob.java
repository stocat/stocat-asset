package com.stocat.asset.scraper.stock.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stocat.asset.scraper.stock.dto.StockPriceDto;
import com.stocat.asset.scraper.stock.service.NasdaqScrapeService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockJob {

    private final NasdaqScrapeService scraperService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper; // 생성자 주입됨

    @Value("${stock.tickers:AAPL,TSLA,NVDA,MSFT,AMZN}")
    private List<String> targetTickers;

    @Value("${stock.redis.channel:stock-updates}")
    private String redisChannel;

    @Value("${stock.redis.polling-interval:PT3S}")
    private Duration pollingInterval;

    @PostConstruct
    public void start() {
        scraperService.streamQuotes(targetTickers, pollingInterval)
            .flatMap(this::publish)
            .doOnSubscribe(sub -> log.info("US stock stream started - tickers={}, interval={}", targetTickers, pollingInterval))
            .doOnError(error -> log.error("US stock stream error", error))
            .subscribe();
    }

    private Mono<Void> publish(StockPriceDto data) {
        return Mono.fromCallable(() -> objectMapper.writeValueAsString(data))
            .doOnNext(jsonMessage -> redisTemplate.convertAndSend(redisChannel, jsonMessage))
            .doOnError(error -> log.error("US stock publish failed - ticket={}", data.getTicker(), error))
            .onErrorResume(error -> Mono.empty())
            .then()
            .subscribeOn(Schedulers.boundedElastic());
    }
}

