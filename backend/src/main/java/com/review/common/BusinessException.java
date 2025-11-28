package com.review.common;

import lombok.Getter;

/**
 * 业务异常
 *
 * @author Claude
 * @date 2025-11-28
 */
@Getter
public class BusinessException extends RuntimeException {

    private final String code;

    public BusinessException(String message) {
        super(message);
        this.code = "BUSINESS_ERROR";
    }

    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
    }
}
