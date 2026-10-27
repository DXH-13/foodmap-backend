package com.foodmap.category.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/** Khớp schema {@code Category} trong docs/SDD/api/openapi.yaml. */
@Schema(description = "Danh mục món ăn")
public record CategoryDto(
        UUID id,
        String slug,
        @Schema(description = "Đã dịch; thiếu bản en thì trả bản vi") String name,
        String icon,
        int displayOrder
) {
}
