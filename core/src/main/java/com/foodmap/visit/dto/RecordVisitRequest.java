package com.foodmap.visit.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/**
 * Khớp schema {@code RecordVisitRequest} trong docs/SDD/api/openapi.yaml.
 * Toạ độ dùng {@code Double} để thiếu trường là lỗi 400, không âm thầm thành 0.
 */
public record RecordVisitRequest(@NotNull @Valid Coordinates coordinates) {

    public record Coordinates(
            @NotNull @DecimalMin("-90") @DecimalMax("90") Double latitude,
            @NotNull @DecimalMin("-180") @DecimalMax("180") Double longitude) {
    }
}
