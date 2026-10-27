package com.foodmap.place.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/** Khớp schema {@code PlaceExternalLink} trong docs/SDD/api/openapi.yaml. */
public record PlaceExternalLinkDto(
        UUID id,
        @Schema(allowableValues = {"TIKTOK", "FACEBOOK", "INSTAGRAM", "YOUTUBE", "OTHER"}) String platform,
        @Schema(description = "Luôn bắt đầu bằng https://") String url,
        @Schema(description = "null thì client hiện tên nền tảng") String title
) {
}
