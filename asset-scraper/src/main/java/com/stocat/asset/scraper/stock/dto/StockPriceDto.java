package com.stocat.asset.scraper.stock.dto;

import com.stocat.asset.mysql.domain.asset.domain.AssetsCategory;
import com.stocat.asset.mysql.domain.asset.domain.Currency;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StockPriceDto {
    private String ticker;
    private BigDecimal price;
    private Double changeRate;
    private String state;

    private AssetsCategory category;
    private Currency currency;
}