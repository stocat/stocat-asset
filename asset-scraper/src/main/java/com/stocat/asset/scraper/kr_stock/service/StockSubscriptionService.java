package com.stocat.asset.scraper.kr_stock.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stocat.asset.mysql.domain.asset.domain.AssetsCategory;
import com.stocat.asset.mysql.domain.asset.repository.AssetsRepository;
import com.stocat.asset.redis.constants.StockKeys;
import com.stocat.asset.scraper.service.SubscriptionCodeService;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class StockSubscriptionService extends SubscriptionCodeService {

    public StockSubscriptionService(
            ReactiveStringRedisTemplate redisTemplate,
            AssetsRepository assetsRepository,
            ObjectMapper mapper) {
        super(
                redisTemplate,
                assetsRepository,
                mapper,
                StockKeys.STOCK_SUBSCRIBE_CODES,
                StockKeys.STOCK_HOT_CODES,
                StockKeys.STOCK_TRADES,
                AssetsCategory.KOR_STOCK
        );
    }
}