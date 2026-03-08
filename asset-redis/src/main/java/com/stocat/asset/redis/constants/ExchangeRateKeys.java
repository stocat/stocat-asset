package com.stocat.asset.redis.constants;

public final class ExchangeRateKeys {

    private ExchangeRateKeys() {
    }

    public static final String EXCHANGE_RATES = "exchange:rates";

    private static final String EXCHANGE_RATE_PREFIX = "exchange:rate:";

    public static String rateKey(String currencyPair) {
        return EXCHANGE_RATE_PREFIX + currencyPair;
    }
}
