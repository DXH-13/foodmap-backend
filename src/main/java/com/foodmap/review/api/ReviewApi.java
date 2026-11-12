package com.foodmap.review.api;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
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

    /**
     * Thông tin tối thiểu của một đánh giá (địa điểm, tác giả, trạng thái) để
     * module khác xử lý sự kiện — ví dụ {@code notification} ở Bước 5 cần biết
     * tác giả là ai để báo kết quả kiểm duyệt, và báo chủ quán khi có đánh giá
     * mới mà không tự báo cho chính tác giả.
     *
     * <p>Rỗng khi đánh giá không tồn tại hoặc đã xoá mềm.
     */
    Optional<ReviewDetails> findReview(UUID reviewId);

    /**
     * Các đánh giá <b>đã duyệt</b> mới nhất của một địa điểm, cho tool
     * {@code list_place_reviews} của chatbot ở Bước 5 — chatbot đọc đánh giá thật
     * để trả lời thay vì tự bịa cảm nhận về quán.
     *
     * @param limit số kết quả tối đa, ép về khoảng 1..10.
     */
    List<ReviewSummary> listApprovedReviews(UUID placeId, int limit);

    /** Bản ghi chỉ đọc của một đánh giá, dùng cho module khác qua {@code ReviewApi}. */
    record ReviewDetails(UUID reviewId, UUID placeId, UUID authorId, ReviewStatus status) {
    }

    /** Đánh giá đã duyệt ở dạng rút gọn, dùng cho module khác qua {@code ReviewApi}. */
    record ReviewSummary(UUID reviewId, int rating, String content, Instant createdAt) {
    }
}
