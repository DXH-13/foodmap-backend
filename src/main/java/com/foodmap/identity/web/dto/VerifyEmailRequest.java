package com.foodmap.identity.web.dto;

import jakarta.validation.constraints.NotBlank;

/** Xác minh email bằng token — schema nội tuyến trong hợp đồng. */
public record VerifyEmailRequest(
        @NotBlank String token
) {
}
