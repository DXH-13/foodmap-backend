package com.foodmap.identity.web.dto;

import jakarta.validation.constraints.NotBlank;

/** Làm mới token, khớp schema {@code RefreshTokenRequest} trong hợp đồng. */
public record RefreshTokenRequest(
        @NotBlank String refreshToken
) {
}
