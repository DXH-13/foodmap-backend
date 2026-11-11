package com.foodmap.identity.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Đăng ký, khớp schema {@code RegisterRequest} trong hợp đồng.
 * Quy tắc mật khẩu đầy đủ (ít nhất một chữ cái và một chữ số) kiểm tra ở service
 * để trả mã lỗi {@code WEAK_PASSWORD}; ở đây chỉ kiểm tra độ dài tối thiểu.
 */
public record RegisterRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 128) String password,
        @NotBlank @Size(max = 100) String displayName,
        @Pattern(regexp = "vi|en") String preferredLocale
) {
}
