package com.foodmap.identity.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Yêu cầu đặt lại mật khẩu, khớp schema {@code ForgotPasswordRequest}. */
public record ForgotPasswordRequest(
        @NotBlank @Email String email
) {
}
