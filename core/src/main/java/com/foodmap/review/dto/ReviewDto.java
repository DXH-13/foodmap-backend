package com.foodmap.review.dto;

import com.foodmap.media.dto.MediaDto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Khớp schema {@code Review} (kèm {@code ReviewAuthor}, {@code ReviewPlace}) trong docs/SDD/api/openapi.yaml. */
@Schema(description = "Đánh giá của người dùng về một địa điểm")
public record ReviewDto(
        UUID id,
        UUID placeId,
        Author author,
        int rating,
        String content,
        String locale,
        String status,
        @Schema(description = "Chỉ trả về cho tác giả và cho moderator")
        String moderationNote,
        List<MediaDto> media,
        @Schema(description = "Có ở bảng tin và ở GET /me/reviews")
        Place place,
        Instant createdAt,
        Instant updatedAt
) {

    public ReviewDto withMedia(List<MediaDto> newMedia) {
        return new ReviewDto(id, placeId, author, rating, content, locale, status, moderationNote,
                newMedia, place, createdAt, updatedAt);
    }

    @Schema(name = "ReviewAuthor")
    public record Author(UUID id, String displayName, String avatarUrl) {}

    @Schema(name = "ReviewPlace")
    public record Place(UUID id, String name, String placeType, String coverImageUrl) {}
}
