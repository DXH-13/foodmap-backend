package com.foodmap.review.web.dto;

import java.util.UUID;

/** Tác giả của một đánh giá. Khớp schema {@code ReviewAuthor} trong hợp đồng. */
public record ReviewAuthorDto(
        UUID id,
        String displayName,
        String avatarUrl
) {
}
