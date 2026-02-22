package com.stocat.asset.scraper.stock.service;

import com.stocat.asset.scraper.stock.service.quote.StockQuoteProperties;
import com.stocat.asset.scraper.stock.service.quote.StockQuoteProvider;
import com.stocat.asset.scraper.stock.dto.StockPriceDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class NasdaqScrapeService {

    private static final Duration DEFAULT_POLLING_INTERVAL = Duration.ofSeconds(2);
    private static final int MAX_CONCURRENCY = 4;

    private final StockQuoteProvider quoteProvider;

    public Flux<StockPriceDto> streamQuotes(List<String> tickers) {
        return streamQuotes(tickers, DEFAULT_POLLING_INTERVAL);
    }

    public NasdaqScrapeService(List<StockQuoteProvider> providers,
                               StockQuoteProperties stockQuoteProperties) {
        Map<String, StockQuoteProvider> providerMap = providers.stream()
                .collect(Collectors.toMap(
                        p -> p.getProviderId().toLowerCase(),
                        Function.identity()));
        String configured = stockQuoteProperties.getProvider();
        StockQuoteProvider selected = providerMap.get(configured.toLowerCase());
        if (selected == null) {
            log.warn("알 수 없는 stock.quote.provider: {}, 기본값(첫 provider) 사용", configured);
            selected = providers.get(0);
        }
        this.quoteProvider = selected;
        log.info("Stock quote provider 선택됨: {}", this.quoteProvider.getProviderId());
    }

    public Flux<StockPriceDto> streamQuotes(List<String> tickers, Duration pollingInterval) {
        return Flux.interval(pollingInterval)
                .onBackpressureDrop()
                .flatMap(tick -> Flux.fromIterable(tickers)) // 리스트 내 모든 종목 병렬 처리 준비
                .flatMap(quoteProvider::fetchQuote, MAX_CONCURRENCY)
                .filter(dto -> dto.getPrice().compareTo(BigDecimal.ZERO) > 0) // 유효성 검증
                .doOnSubscribe(sub -> log.debug("Nasdaq Polling 스트림 시작 - tickers={}", tickers))
                .doOnNext(dto -> log.debug("Nasdaq 시세 수신: {}", dto))
                .doOnError(error -> log.error("Nasdaq 스트림 중 오류 발생", error))
                .subscribeOn(Schedulers.boundedElastic());   // I/O 작업용 스레드 풀 사용
    }

    public Mono<StockPriceDto> scrapeTicker(String ticker) {
        return quoteProvider.fetchQuote(ticker)
                .filter(dto -> dto.getPrice().compareTo(BigDecimal.ZERO) > 0)
                .subscribeOn(Schedulers.boundedElastic());
    }
}