package com.foodmap.notification.web.dto;

import java.util.List;

/**
 * Danh sách thông báo phân trang, khớp schema {@code PageOfNotification} trong
 * hợp đồng: các trường của {@code PageMeta} cộng thêm {@code unreadCount} là
 * tổng số thông báo chưa đọc của người dùng (không chỉ trong trang hiện tại).
 */
public record NotificationListResponse(
        List<NotificationDto> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        long unreadCount
) {
}
