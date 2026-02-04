package com.stocat.asset.scraper.stock.service.quote;

import com.stocat.asset.scraper.stock.dto.StockPriceDto;
import reactor.core.publisher.Mono;

public interface StockQuoteProvider {
    String getProviderId();
    Mono<StockPriceDto> fetchQuote(String ticker);
}
