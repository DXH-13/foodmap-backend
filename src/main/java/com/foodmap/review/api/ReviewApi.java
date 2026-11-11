package com.foodmap.review.api;

import java.util.UUID;

/**
 * Giao diện công khai của module {@code review} cho module khác gọi qua {@code api/}.
 *
 * <p>Hiện chỉ có một phương thức: module {@code place} cần số liệu này để cập
 * nhật các cột dẫn xuất ({@code average_rating}, {@code review_count}) mà không
 * được đọc thẳng bảng {@code reviews}.
 */
public interface ReviewApi {

    /**
     * Điểm trung bình và số lượng đánh giá <b>đã duyệt</b> của một địa điểm
     * (đánh giá chưa duyệt, bị từ chối, bị ẩn hay đã xoá mềm đều không tính).
     */
    ReviewStats statsForPlace(UUID placeId);
}
