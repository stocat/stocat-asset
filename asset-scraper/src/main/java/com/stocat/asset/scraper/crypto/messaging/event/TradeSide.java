package com.stocat.asset.scraper.crypto.messaging.event;

import com.stocat.asset.scraper.crypto.exception.AssetScraperErrorCode;
import com.stocat.common.exception.ApiException;

/**
 * 매매 구분
 */
public enum TradeSide {
    BUY,
    SELL;

    public static TradeSide fromUpbitSide(String side) {
        if (side.equals("ASK")) {
            return SELL;
        } else if (side.equals("BID")) {
            return BUY;
        }

        throw new ApiException(AssetScraperErrorCode.INVALID_UPBIT_SIDE);
    }
}