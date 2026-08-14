package com.foodmap.common.exception;

import org.springframework.http.HttpStatus;

/** 400 — yêu cầu không hợp lệ về mặt nghiệp vụ (validation cú pháp đã qua). */
public class BadRequestException extends BusinessException {

    public BadRequestException(String code, String messageKey, Object... messageArgs) {
        super(HttpStatus.BAD_REQUEST, code, messageKey, messageArgs);
    }
}
