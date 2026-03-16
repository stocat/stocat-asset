package com.stocat.asset.redis.constants;

/**
 * Redis key/채널 상수 - 한국 주식 관련.
 */
public final class KrStockKeys {

    public static final String KR_STOCK_TRADES = "kr_stock:trades";
    public static final String KR_STOCK_HOT_CODES = "kr_stock:hot_codes";
    public static final String KR_STOCK_SUBSCRIBE_CODES = "kr_stock:subscribe_codes";
    private KrStockKeys() {
    }
}
