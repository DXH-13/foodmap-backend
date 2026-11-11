package com.foodmap.place.api;

import java.util.UUID;

/**
 * Thông tin tối thiểu về một địa điểm mà module khác cần để làm việc với nó:
 * tên hiển thị, toạ độ và trạng thái.
 *
 * <p>Tên đã áp dụng fallback locale (bản gọi → tiếng Việt), giống
 * {@code PlaceSummaryDto}; không bao giờ {@code null}.
 *
 * @param status tên của {@code PlaceStatus} (DRAFT, PUBLISHED, ...)
 */
public record PlaceDetails(
        UUID id,
        String name,
        double latitude,
        double longitude,
        String status
) {
}
