package com.foodmap.visit.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** Ghi nhận đã đến một địa điểm. Khớp schema {@code RecordVisitRequest} trong hợp đồng. */
public record RecordVisitRequest(
        @NotNull @Valid CoordinatesDto coordinates
) {
}
