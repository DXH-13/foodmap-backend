package com.foodmap.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Khớp schema {@code RegisterRequest} trong docs/SDD/api/openapi.yaml.
 *
 * <p>Độ mạnh mật khẩu (≥ 8 ký tự, có chữ và số — FR-AUTH-01) kiểm ở {@code AuthService}
 * để cả luật trả về <b>một</b> mã lỗi {@code WEAK_PASSWORD} với thông báo đã dịch.
 */
public record RegisterRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(max = 100) String password,
        @NotBlank @Size(max = 100) String displayName,
        @Schema(allowableValues = {"vi", "en"}, description = "Không gửi thì mặc định vi")
        @Pattern(regexp = "vi|en") String preferredLocale
) {
}
