package com.foodmap.review.infrastructure;

import com.foodmap.review.api.ReviewStatus;
import com.foodmap.review.domain.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    Optional<Review> findByIdAndDeletedAtIsNull(UUID id);

    /** Một người chỉ có một đánh giá (chưa xoá) cho mỗi địa điểm — gửi lại là cập nhật. */
    Optional<Review> findByPlaceIdAndUserIdAndDeletedAtIsNull(UUID placeId, UUID userId);

    Page<Review> findByUserIdAndDeletedAtIsNull(UUID userId, Pageable pageable);

    /**
     * Đánh giá công khai của một địa điểm: chỉ review đã duyệt, cộng thêm review
     * của chính người gọi (mọi trạng thái) khi người gọi đã đăng nhập.
     * Truyền {@code callerId = null} cho khách vãng lai.
     */
    @Query("""
            SELECT r FROM Review r
            WHERE r.placeId = :placeId AND r.deletedAt IS NULL
              AND (r.status = com.foodmap.review.api.ReviewStatus.APPROVED
                   OR r.userId = :callerId)
            """)
    Page<Review> findVisibleByPlaceId(@Param("placeId") UUID placeId,
                                      @Param("callerId") UUID callerId,
                                      Pageable pageable);

    /** Hàng chờ kiểm duyệt: lọc theo trạng thái và/hoặc địa điểm, bỏ trống là không lọc. */
    @Query("""
            SELECT r FROM Review r
            WHERE r.deletedAt IS NULL
              AND (:status IS NULL OR r.status = :status)
              AND (:placeId IS NULL OR r.placeId = :placeId)
            """)
    Page<Review> findForModeration(@Param("status") ReviewStatus status,
                                   @Param("placeId") UUID placeId,
                                   Pageable pageable);

    /** Số đánh giá của một tác giả đã từng bị từ chối — ngữ cảnh cho moderator. */
    long countByUserIdAndStatusAndDeletedAtIsNull(UUID userId, ReviewStatus status);

    /**
     * Đánh giá đã duyệt của một địa điểm, mới nhất trước — cho tool
     * {@code list_place_reviews} của chatbot ở Bước 5.
     */
    java.util.List<Review> findByPlaceIdAndStatusAndDeletedAtIsNull(
            UUID placeId, ReviewStatus status, Pageable pageable);

    /**
     * Điểm trung bình và số lượng đánh giá <b>đã duyệt</b> của một địa điểm.
     * Trả {@code [AVG, COUNT]}; AVG là {@code null} khi chưa có đánh giá nào.
     */
    @Query("""
            SELECT AVG(r.rating), COUNT(r) FROM Review r
            WHERE r.placeId = :placeId
              AND r.status = com.foodmap.review.api.ReviewStatus.APPROVED
              AND r.deletedAt IS NULL
            """)
    Object[] aggregateApprovedByPlaceId(@Param("placeId") UUID placeId);
}
