package com.foodmap.place;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Kết quả thô của các truy vấn trả về {@code PlaceSummary} (quanh đây, từ khoá, yêu thích).
 *
 * <p>Dùng projection thay vì nạp cả entity: truy vấn này chạy ở mỗi lần người dùng
 * mở app hoặc di chuyển bản đồ, nên chỉ lấy đúng những cột cần hiển thị.
 */
public interface PlaceSummaryProjection {

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

    /** Khoá object của ảnh bìa, {@code null} khi chưa có. */
    String getCoverKey();

    /** {@code null} khi truy vấn không tính theo vị trí. */
    Double getDistanceMeters();
}
