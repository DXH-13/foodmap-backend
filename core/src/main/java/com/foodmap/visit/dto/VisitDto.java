package com.foodmap.visit.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Khớp schema {@code Visit} trong docs/SDD/api/openapi.yaml. */
@Schema(description = "Một lượt đến địa điểm")
public record VisitDto(
        UUID id,
        UUID placeId,
        @Schema(description = "Đã dịch; thiếu bản en thì trả bản vi") String placeName,
        @Schema(description = "Ngày theo giờ Asia/Ho_Chi_Minh") LocalDate visitDate,
        int distanceMeters,
        Instant createdAt
) {
}
