package com.foodmap.notification.web.dto;

import com.foodmap.notification.api.NotificationType;

import java.time.Instant;
import java.util.UUID;

/**
 * Một thông báo trả ra API, khớp schema {@code Notification} trong hợp đồng.
 * {@code title} đã được dịch theo ngôn ngữ ưa thích của người nhận;
 * {@code body} hiện luôn {@code null} (chưa có bản dịch body cho các loại này).
 */
public record NotificationDto(
        UUID id,
        NotificationType type,
        String title,
        String body,
        UUID placeId,
        UUID reviewId,
        boolean isRead,
        Instant createdAt
) {
}
