package com.foodmap.common.exception;

import org.springframework.http.HttpStatus;

/** 404 — không tìm thấy tài nguyên. */
public class NotFoundException extends BusinessException {

    public NotFoundException(String code, String messageKey, Object... messageArgs) {
        super(HttpStatus.NOT_FOUND, code, messageKey, messageArgs);
    }
}
