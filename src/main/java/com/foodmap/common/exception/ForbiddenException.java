package com.foodmap.common.exception;

import org.springframework.http.HttpStatus;

/** 403 — đã xác thực nhưng không đủ quyền. */
public class ForbiddenException extends BusinessException {

    public ForbiddenException(String code, String messageKey, Object... messageArgs) {
        super(HttpStatus.FORBIDDEN, code, messageKey, messageArgs);
    }
}
