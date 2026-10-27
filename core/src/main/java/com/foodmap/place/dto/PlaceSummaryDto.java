package com.foodmap.place.dto;

import com.foodmap.category.dto.CategoryDto;
import com.foodmap.place.PlaceSummaryProjection;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Địa điểm ở dạng rút gọn, dùng cho danh sách và marker trên bản đồ.
 * Khớp schema {@code PlaceSummary} trong docs/03-api/openapi.yaml.
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
        String coverImageUrl,
        @Schema(description = "Chỉ có mặt ở kết quả tìm theo vị trí")
        Double distanceMeters,
        List<CategoryDto> categories
) {

    public static PlaceSummaryDto from(PlaceSummaryProjection row, String coverImageUrl, List<CategoryDto> categories) {
        Double distance = row.getDistanceMeters();
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
                coverImageUrl,
                // Làm tròn tới 0,1 mét — độ chính xác hơn thế không có ý nghĩa với người dùng
                distance == null ? null : Math.round(distance * 10.0) / 10.0,
                categories);
    }
}
