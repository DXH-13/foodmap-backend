package com.foodmap.place.domain;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Kết quả thô của truy vấn lấy địa điểm rút gọn theo danh sách id (dùng cho
 * danh sách yêu thích). Giống {@link PlaceSummaryProjection} nhưng không có
 * {@code distanceMeters} — khoảng cách do module gọi tự tính theo nhu cầu.
 */
public interface PlaceSummaryRow {

    UUID getId();

    String getSlug();

    String getPlaceType();

    String getStatus();

    /** Đã áp dụng fallback locale ở tầng SQL — không bao giờ null. */
    String getName();

    String getAddress();

    /** {@code null} khi chưa có đánh giá nào. */
    BigDecimal getAverageRating();

    int getReviewCount();

    long getVisitCount();

    double getLatitude();

    double getLongitude();
}
