package com.foodmap.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Khớp schema {@code LoginRequest} trong docs/SDD/api/openapi.yaml. */
public record LoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password
) {
}
