package com.stocat.asset.scraper.stock.service.quote;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.stocat.asset.mysql.domain.asset.domain.AssetsCategory;
import com.stocat.asset.mysql.domain.asset.domain.Currency;
import com.stocat.asset.scraper.stock.config.AlphaVantageApiProperties;
import com.stocat.asset.scraper.stock.dto.StockPriceDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlphaVantageQuoteProvider implements StockQuoteProvider {

    @Qualifier("alphaVantageWebClient")
    private final WebClient alphaVantageWebClient;
    private final AlphaVantageApiProperties alphaVantageApiProperties;

    @Override
    public String getProviderId() {
        return "alpha";
    }

    @Override
    public Mono<StockPriceDto> fetchQuote(String ticker) {
        return alphaVantageWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/query")
                        .queryParam("function", "GLOBAL_QUOTE")
                        .queryParam("symbol", ticker)
                        .queryParam("apikey", alphaVantageApiProperties.getApiKey())
                        .build())
                .retrieve()
                .bodyToMono(AlphaVantageGlobalQuoteResponse.class)
                .map(response -> toStockPriceDto(ticker, response))
                .onErrorResume(e -> {
                    log.warn("Alpha Vantage 호출 실패 - ticker: {}, error: {}", ticker, e.getMessage());
                    return Mono.empty();
                });
    }

    private StockPriceDto toStockPriceDto(String ticker, AlphaVantageGlobalQuoteResponse response) {
        GlobalQuote quote = response != null ? response.globalQuote() : null;
        String priceText = quote != null ? quote.price() : null;
        String changePercentText = quote != null ? quote.changePercent() : null;

        if (priceText != null && !priceText.isEmpty()) {
            log.debug("디버깅 - Ticker: {}, Alpha Vantage price: {}", ticker, priceText);
        } else {
            log.warn("디버깅 - Ticker: {}, 가격 정보 없음", ticker);
        }

        BigDecimal price = parsePriceText(priceText);
        Double changeRate = parseChangeRateText(changePercentText);

        return StockPriceDto.builder()
                .ticker(ticker)
                .price(price)
                .changeRate(changeRate)
                .state("REGULAR")
                .category(AssetsCategory.US_STOCK)
                .currency(Currency.USD)
                .build();
    }

    private BigDecimal parsePriceText(String text) {
        try {
            if (text == null) return BigDecimal.ZERO;
            String cleanText = text.replaceAll("[^0-9.]", "").trim();
            if (cleanText.isEmpty()) return BigDecimal.ZERO;
            return new BigDecimal(cleanText);
        } catch (Exception e) {
            log.error("가격 파싱 에러 발생 - 원본: {}, 사유: {}", text, e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    private Double parseChangeRateText(String text) {
        try {
            if (text == null) return 0.0;
            String cleanStr = text.replaceAll("[()%+]", "").trim();
            if (cleanStr.isEmpty()) return 0.0;
            return Double.parseDouble(cleanStr);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private record AlphaVantageGlobalQuoteResponse(
            @JsonProperty("Global Quote") GlobalQuote globalQuote
    ) {}

    private record GlobalQuote(
            @JsonProperty("05. price") String price,
            @JsonProperty("10. change percent") String changePercent
    ) {}
}
