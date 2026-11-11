package com.foodmap.identity.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Đặt lại mật khẩu bằng token, khớp schema {@code ResetPasswordRequest}. */
public record ResetPasswordRequest(
        @NotBlank String token,
        @NotBlank @Size(min = 8, max = 128) String newPassword
) {
}
