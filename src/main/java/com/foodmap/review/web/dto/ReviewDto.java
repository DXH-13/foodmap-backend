package com.foodmap.review.web.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Một đánh giá trả ra API. Khớp schema {@code Review} trong hợp đồng.
 *
 * <p>{@code moderationNote} chỉ có giá trị cho tác giả và moderator — danh sách
 * công khai trả {@code null} cho review của người khác (service quyết định).
 */
public record ReviewDto(
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
        Instant updatedAt
) {
}
