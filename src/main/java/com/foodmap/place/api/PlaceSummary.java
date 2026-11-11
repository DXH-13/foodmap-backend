package com.foodmap.place.api;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Địa điểm ở dạng rút gọn, dùng cho module khác (ví dụ danh sách yêu thích của
 * {@code favorite}) khi cần hiển thị mà không cần tải toàn bộ entity.
 *
 * <p>Hình dạng khớp schema {@code PlaceSummary} trong hợp đồng, trừ
 * {@code distanceMeters} và {@code categories} — khoảng cách do module gọi tự
 * tính theo nhu cầu của nó, danh mục không thuộc phạm vi dùng chung này.
 */
public record PlaceSummary(
        UUID id,
        String slug,
        String name,
        String placeType,
        double latitude,
        double longitude,
        String address,
        String status,
        BigDecimal averageRating,
        int reviewCount,
        long visitCount
) {
}
