package com.stocat.asset.scraper.crypto.exception;

import com.stocat.asset.core.exception.ErrorCode;
import com.stocat.asset.core.exception.ErrorDomain;

public enum AssetScraperErrorCode implements ErrorCode {
    INTERNAL_ERROR(ErrorDomain.ASSET_SCRAPER.offset(), "서버 에러가 발생했습니다."),
    INVALID_UPBIT_SIDE(ErrorDomain.ASSET_SCRAPER.offset() + 1, "업비트 체결 구분 값이 올바르지 않습니다.")
    ;

    private final int code;
    private final String message;

    AssetScraperErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public int code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }
}