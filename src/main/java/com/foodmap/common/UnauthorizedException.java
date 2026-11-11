package com.foodmap.common;

import org.springframework.http.HttpStatus;

/**
 * 401 — chưa đăng nhập hoặc token/phiên không hợp lệ.
 *
 * <p>Đặt ở package gốc {@code common} (cùng chỗ với {@link BadRequestException}) để
 * module khác được phép dùng — xem ghi chú ở Bước 2: Modulith chỉ cho module khác
 * dùng type ở package gốc của module, không cho dùng type trong package con.
 */
public class UnauthorizedException extends BusinessException {

    public UnauthorizedException(String code, String messageKey, Object... messageArgs) {
        super(HttpStatus.UNAUTHORIZED, code, messageKey, messageArgs);
    }
}
