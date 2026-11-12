package com.foodmap.notification.web.dto;

import com.foodmap.notification.api.NotificationType;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Cài đặt nhận thông báo, khớp schema {@code NotificationSettings} trong hợp
 * đồng. Client luôn gửi danh sách đầy đủ các loại được phép khi cập nhật.
 */
public record NotificationSettingsDto(
        @NotNull List<NotificationType> enabledTypes
) {
}
