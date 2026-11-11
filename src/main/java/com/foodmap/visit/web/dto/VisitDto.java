package com.foodmap.visit.web.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Một lượt đến trả ra API. Khớp schema {@code Visit} trong hợp đồng.
 *
 * <p>{@code placeName} có thể {@code null} khi địa điểm đã bị xoá mềm sau lúc
 * ghi nhận — tên lúc đó không còn tra được.
 */
public record VisitDto(
        UUID id,
        UUID placeId,
        String placeName,
        LocalDate visitDate,
        int distanceMeters,
        Instant createdAt
) {
}
