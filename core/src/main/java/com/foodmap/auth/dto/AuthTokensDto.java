package com.foodmap.auth.dto;

import com.foodmap.user.dto.UserProfileDto;
import io.swagger.v3.oas.annotations.media.Schema;

/** Khớp schema {@code AuthTokens} trong docs/SDD/api/openapi.yaml. */
@Schema(description = "Cặp token và hồ sơ người dùng vừa đăng nhập")
public record AuthTokensDto(
        String accessToken,
        String refreshToken,
        @Schema(description = "Thời gian sống của access token, đơn vị giây")
        long expiresIn,
        UserProfileDto user
) {
}
