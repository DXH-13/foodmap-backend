package com.foodmap.common;

import org.springframework.http.HttpStatus;

/** 429 — vượt giới hạn số lần gọi (ví dụ chatbot: 30 tin nhắn / người / giờ). */
public class TooManyRequestsException extends BusinessException {

    public TooManyRequestsException(String code, String messageKey, Object... messageArgs) {
        super(HttpStatus.TOO_MANY_REQUESTS, code, messageKey, messageArgs);
    }
}
