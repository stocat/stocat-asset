package com.stocat.asset.scraper.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stocat.asset.mysql.domain.asset.domain.AssetsCategory;
import com.stocat.asset.mysql.domain.asset.repository.AssetsRepository;
import com.stocat.asset.redis.constants.CryptoKeys;
import com.stocat.asset.scraper.service.SubscriptionCodeService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;

@Configuration
public class ScraperServiceConfig {

    @Bean
    public SubscriptionCodeService cryptoSubscriptionService(
            ReactiveStringRedisTemplate redisTemplate,
            AssetsRepository assetsRepository,
            ObjectMapper mapper) {
        return new SubscriptionCodeService(
                redisTemplate,
                assetsRepository,
                mapper,
                CryptoKeys.CRYPTO_SUBSCRIBE_CODES,
                CryptoKeys.CRYPTO_HOT_CODES,
                CryptoKeys.CRYPTO_TRADES,
                AssetsCategory.CRYPTO
        );
    }
}