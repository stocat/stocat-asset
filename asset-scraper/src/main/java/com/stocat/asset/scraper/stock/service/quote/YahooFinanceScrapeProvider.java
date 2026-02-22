package com.stocat.asset.scraper.stock.service.quote;

import com.stocat.asset.mysql.domain.asset.domain.AssetsCategory;
import com.stocat.asset.mysql.domain.asset.domain.Currency;
import com.stocat.asset.scraper.stock.dto.StockPriceDto;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Slf4j
@Service
public class YahooFinanceScrapeProvider implements StockQuoteProvider {

    private static final String BASE_URL = "https://finance.yahoo.com/quote/";
    private static final int TIMEOUT_MS = 5000;

    @Override
    public String getProviderId() {
        return "yahoo";
    }

    @Override
    public Mono<StockPriceDto> fetchQuote(String ticker) {
        return Mono.fromCallable(() -> {
                    Document doc = Jsoup.connect(BASE_URL + ticker)
                            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                            .timeout(TIMEOUT_MS)
                            .get();
                    return new DocumentWrapper(ticker, doc);
                })
                .map(this::toStockPriceDto)
                .onErrorResume(e -> {
                    log.warn("Yahoo 스크래핑 실패 - ticker: {}, error: {}", ticker, e.getMessage());
                    return Mono.empty();
                });
    }

    private StockPriceDto toStockPriceDto(DocumentWrapper wrapper) {
        Document doc = wrapper.document();
        String ticker = wrapper.ticker();

        Element priceElem = doc.selectFirst("fin-streamer[data-field='regularMarketPrice']");
        Element changeElem = doc.selectFirst("fin-streamer[data-field='regularMarketChangePercent']");
        Element prePriceElem = doc.selectFirst("fin-streamer[data-field='preMarketPrice']");
        Element postPriceElem = doc.selectFirst("fin-streamer[data-field='postMarketPrice']");

        if (priceElem != null) {
            log.debug("디버깅 - Ticker: {}, 추출된 원본 텍스트: {}", ticker, priceElem.text());
        } else {
            log.warn("디버깅 - Ticker: {}, 가격 요소를 찾을 수 없음!", ticker);
        }

        BigDecimal price = BigDecimal.ZERO;
        Double changeRate = 0.0;
        String state = "REGULAR";

        // 우선순위: Post Market -> Pre Market -> Regular
        if (isValid(postPriceElem)) {
            price = parsePrice(postPriceElem);
            state = "POST";
        } else if (isValid(prePriceElem)) {
            price = parsePrice(prePriceElem);
            state = "PRE";
        } else if (isValid(priceElem)) {
            price = parsePrice(priceElem);
            if (changeElem != null) {
                changeRate = parseChangeRate(changeElem);
            }
        }

        return StockPriceDto.builder()
                .ticker(ticker)
                .price(price)
                .changeRate(changeRate)
                .state(state)
                .category(AssetsCategory.US_STOCK)
                .currency(Currency.USD)
                .build();
    }

    private boolean isValid(Element elem) {
        return elem != null && isValidText(elem.text());
    }

    private boolean isValidText(String text) {
        return text != null && !text.isEmpty() && !text.equals("--");
    }

    private BigDecimal parsePrice(Element elem) {
        try {
            String cleanText = elem.text().replaceAll("[^0-9.]", "").trim();
            if (cleanText.isEmpty()) return BigDecimal.ZERO;
            return new BigDecimal(cleanText);
        } catch (Exception e) {
            log.error("가격 파싱 에러 발생 - 원본: {}, 사유: {}", elem.text(), e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    private Double parseChangeRate(Element elem) {
        try {
            String cleanStr = elem.text().replaceAll("[()%+]", "").trim();
            return Double.parseDouble(cleanStr);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private record DocumentWrapper(String ticker, Document document) {}
}
