package com.foodmap.place.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Toạ độ ở dạng API phơi ra: <b>vĩ độ trước</b> — ngược với thứ tự của PostGIS.
 * Việc chuyển đổi chỉ được làm qua {@code GeoUtils}.
 */
@Schema(description = "Toạ độ địa lý (WGS-84)")
public record CoordinatesDto(
        @Schema(example = "10.8231") double latitude,
        @Schema(example = "106.6297") double longitude
) {
}
