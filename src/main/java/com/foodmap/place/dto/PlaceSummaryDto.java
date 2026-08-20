package com.foodmap.place.dto;

import com.foodmap.place.PlaceSummaryProjection;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Địa điểm ở dạng rút gọn, dùng cho danh sách và marker trên bản đồ.
 * Khớp schema {@code PlaceSummary} trong docs/SDD/api/openapi.yaml.
 */
@Schema(description = "Địa điểm ở dạng rút gọn")
public record PlaceSummaryDto(
        UUID id,
        String slug,
        String name,
        String placeType,
        CoordinatesDto coordinates,
        String address,
        String status,
        @Schema(description = "null khi chưa có đánh giá nào — không phải 0")
        BigDecimal averageRating,
        int reviewCount,
        long visitCount,
        @Schema(description = "Chỉ có mặt ở kết quả tìm theo vị trí")
        Double distanceMeters
) {

    public static PlaceSummaryDto from(PlaceSummaryProjection row) {
        return new PlaceSummaryDto(
                row.getId(),
                row.getSlug(),
                row.getName(),
                row.getPlaceType(),
                new CoordinatesDto(row.getLatitude(), row.getLongitude()),
                row.getAddress(),
                row.getStatus(),
                row.getAverageRating(),
                row.getReviewCount(),
                row.getVisitCount(),
                // Làm tròn tới mét — độ chính xác dưới mét không có ý nghĩa với người dùng
                Math.round(row.getDistanceMeters() * 10.0) / 10.0);
    }
}
