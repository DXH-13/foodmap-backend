package com.foodmap.common;

import org.springframework.http.HttpStatus;

/** 503 — dịch vụ ngoài tạm thời không khả dụng (ví dụ chatbot chưa có key Gemini). */
public class ServiceUnavailableException extends BusinessException {

    public ServiceUnavailableException(String code, String messageKey, Object... messageArgs) {
        super(HttpStatus.SERVICE_UNAVAILABLE, code, messageKey, messageArgs);
    }
}
