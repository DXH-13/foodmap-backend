package com.foodmap.notification.web.dto;

import com.foodmap.notification.api.Platform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Body của {@code POST /app/v1/me/push-tokens}, khớp schema cùng tên trong hợp đồng. */
public record RegisterPushTokenRequest(
        @NotBlank String token,
        @NotNull Platform platform
) {
}
