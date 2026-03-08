package com.stocat.asset.redis.constants;

/**
 * Redis key/채널 상수 - 한국 주식 관련.
 */
public final class StockKeys {

    private StockKeys() {
    }

    public static final String STOCK_TRADES = "stock:trades";
    public static final String STOCK_HOT_CODES = "stock:hot_codes";
    public static final String STOCK_SUBSCRIBE_CODES = "stock:subscribe_codes";
}
