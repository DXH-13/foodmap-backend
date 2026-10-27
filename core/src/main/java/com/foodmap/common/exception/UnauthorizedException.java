package com.foodmap.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 401 — sai thông tin đăng nhập hoặc refresh token không dùng được.
 *
 * <p>Khác lỗi 401 do thiếu access token (do {@code RestAuthenticationHandlers} trả về):
 * lỗi này có {@code code} riêng để client phân biệt "sai mật khẩu" với "chưa đăng nhập".
 */
public class UnauthorizedException extends BusinessException {

    public UnauthorizedException(String code, String messageKey, Object... messageArgs) {
        super(HttpStatus.UNAUTHORIZED, code, messageKey, messageArgs);
    }
}
