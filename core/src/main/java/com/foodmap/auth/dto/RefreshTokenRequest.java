package com.foodmap.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** Khớp schema {@code RefreshTokenRequest} trong docs/SDD/api/openapi.yaml. */
public record RefreshTokenRequest(@NotBlank String refreshToken) {
}
