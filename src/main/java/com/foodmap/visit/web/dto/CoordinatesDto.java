package com.foodmap.visit.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** Toạ độ hiện tại của người gọi. Khớp schema {@code Coordinates} trong hợp đồng. */
public record CoordinatesDto(
        @Min(-90) @Max(90) double latitude,
        @Min(-180) @Max(180) double longitude
) {
}
