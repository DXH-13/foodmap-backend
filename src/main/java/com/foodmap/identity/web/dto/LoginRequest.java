package com.foodmap.identity.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Đăng nhập, khớp schema {@code LoginRequest} trong hợp đồng. */
public record LoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password
) {
}
