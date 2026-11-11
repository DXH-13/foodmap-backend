package com.foodmap.review.web.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Một đánh giá trong hàng chờ kiểm duyệt. Khớp schema {@code ModerationReview}
 * trong hợp đồng (mở rộng {@code Review} thêm tên địa điểm và số lần tác giả
 * đã bị từ chối trước đó).
 */
public record ModerationReviewDto(
        UUID id,
        UUID placeId,
        ReviewAuthorDto author,
        int rating,
        String content,
        String locale,
        String status,
        String moderationNote,
        List<ReviewMediaDto> media,
        Instant createdAt,
        Instant updatedAt,
        String placeName,
        long authorRejectedCount
) {

    public static ModerationReviewDto from(ReviewDto review, String placeName,
                                           long authorRejectedCount) {
        return new ModerationReviewDto(
                review.id(),
                review.placeId(),
                review.author(),
                review.rating(),
                review.content(),
                review.locale(),
                review.status(),
                review.moderationNote(),
                review.media(),
                review.createdAt(),
                review.updatedAt(),
                placeName,
                authorRejectedCount);
    }
}
