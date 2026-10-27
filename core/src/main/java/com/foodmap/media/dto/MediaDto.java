package com.foodmap.media.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/** Khớp schema {@code Media} trong docs/SDD/api/openapi.yaml. */
@Schema(description = "Ảnh hoặc video đã upload")
public record MediaDto(
        UUID id,
        String mediaType,
        @Schema(description = "Bản hiển thị, cạnh dài 1600px") String url,
        @Schema(description = "Bản 400px") String thumbnailUrl,
        Integer width,
        Integer height,
        Integer durationSeconds
) {
}
