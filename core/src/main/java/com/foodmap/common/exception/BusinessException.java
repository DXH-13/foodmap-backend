package com.foodmap.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Gốc của mọi lỗi nghiệp vụ.
 *
 * <p>Mang theo <b>mã lỗi</b> (ổn định, không dịch) và <b>key i18n</b> của thông báo
 * (được dịch lúc dựng response). Tách hai thứ này là chủ ý: client phân nhánh theo
 * {@code code}, người dùng đọc {@code message}.
 */
public class BusinessException extends RuntimeException {

    private final String code;
    private final String messageKey;
    private final transient Object[] messageArgs;
    private final HttpStatus status;

    protected BusinessException(HttpStatus status, String code, String messageKey, Object... messageArgs) {
        super(code);
        this.status = status;
        this.code = code;
        this.messageKey = messageKey;
        this.messageArgs = messageArgs;
    }

    public String getCode() {
        return code;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public Object[] getMessageArgs() {
        return messageArgs;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
