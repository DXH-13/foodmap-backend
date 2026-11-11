package com.foodmap.identity.web.dto;

import com.foodmap.identity.api.UserProfile;
import com.foodmap.identity.application.AuthService;

/** Cặp token trả về sau đăng ký / đăng nhập / làm mới, khớp schema {@code AuthTokens}. */
public record AuthTokensResponse(
        String accessToken,
        String refreshToken,
        long expiresIn,
        UserProfile user
) {

    public static AuthTokensResponse from(AuthService.AuthResult result) {
        return new AuthTokensResponse(
                result.accessToken(), result.refreshToken(), result.expiresIn(), result.user());
    }
}
