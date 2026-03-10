package com.stocat.asset.scraper.crypto.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stocat.asset.mysql.domain.asset.domain.AssetsCategory;
import com.stocat.asset.mysql.domain.asset.repository.AssetsRepository;
import com.stocat.asset.redis.constants.CryptoKeys;
import com.stocat.asset.scraper.service.SubscriptionCodeService;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class CryptoSubscriptionService extends SubscriptionCodeService {

    public CryptoSubscriptionService(
            ReactiveStringRedisTemplate redisTemplate,
            AssetsRepository assetsRepository,
            ObjectMapper mapper) {
        super(
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