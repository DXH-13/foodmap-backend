package com.foodmap.review.web.dto;

import java.util.UUID;

/**
 * Media đính kèm một đánh giá. Khớp schema {@code Media} trong hợp đồng.
 *
 * <p>Hiện luôn trả danh sách rỗng — luồng upload media thuộc bước sau.
 */
public record ReviewMediaDto(
        UUID id,
        String mediaType,
        String url,
        String thumbnailUrl,
        Double width,
        Double height,
        Double durationSeconds
) {
}
