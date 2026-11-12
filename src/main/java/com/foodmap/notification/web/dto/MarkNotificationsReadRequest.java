package com.foodmap.notification.web.dto;

import java.util.List;
import java.util.UUID;

/**
 * Body của {@code POST /app/v1/me/notifications/read}. Danh sách rỗng hoặc
 * không truyền nghĩa là đánh dấu tất cả đã đọc (theo mô tả operation trong
 * hợp đồng).
 */
public record MarkNotificationsReadRequest(
        List<UUID> notificationIds
) {
}
