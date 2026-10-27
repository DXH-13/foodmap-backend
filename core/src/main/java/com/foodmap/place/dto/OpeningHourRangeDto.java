package com.foodmap.place.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Khớp schema {@code OpeningHourRange} trong docs/SDD/api/openapi.yaml. */
public record OpeningHourRangeDto(
        @Schema(description = "1 = thứ Hai … 7 = Chủ nhật (ISO-8601)") int dayOfWeek,
        @Schema(description = "HH:mm, null khi nghỉ cả ngày") String openTime,
        @Schema(description = "HH:mm, nhỏ hơn openTime nghĩa là qua nửa đêm") String closeTime,
        boolean isClosedAllDay
) {
}
