package com.foodmap.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 409 — xung đột với trạng thái hiện tại.
 *
 * <p>Dùng cho: email đã đăng ký, feedback cùng loại đang mở, đã check-in trong ngày,
 * đánh giá đã được người khác kiểm duyệt.
 */
public class ConflictException extends BusinessException {

    public ConflictException(String code, String messageKey, Object... messageArgs) {
        super(HttpStatus.CONFLICT, code, messageKey, messageArgs);
    }
}
