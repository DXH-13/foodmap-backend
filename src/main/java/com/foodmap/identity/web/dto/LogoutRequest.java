package com.foodmap.identity.web.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Đăng xuất, khớp schema {@code LogoutRequest} trong hợp đồng.
 * {@code pushToken} hiện chỉ được nhận mà chưa xử lý — gỡ push token thuộc về
 * module {@code notification} (Phase 4), xem ghi chú ở {@code AuthService}.
 */
public record LogoutRequest(
        @NotBlank String refreshToken,
        String pushToken
) {
}
