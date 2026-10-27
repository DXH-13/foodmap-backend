package com.foodmap.place.dto;

import com.foodmap.category.dto.CategoryDto;
import com.foodmap.media.dto.MediaDto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Khớp schema {@code PlaceDetail} (= {@code PlaceSummary} + phần chi tiết) trong docs/SDD/api/openapi.yaml. */
@Schema(description = "Chi tiết một địa điểm")
public record PlaceDetailDto(
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
        @Schema(description = "Luôn null ở trang chi tiết — chỉ có ở kết quả tìm theo vị trí")
        Double distanceMeters,
        @Schema(description = "Theo giờ Asia/Ho_Chi_Minh; null khi chưa có dữ liệu giờ mở cửa")
        Boolean isOpenNow,
        List<CategoryDto> categories,
        String description,
        String phone,
        long distinctVisitorCount,
        @Schema(description = "Thời điểm mở cửa tiếp theo khi hiện đang đóng")
        Instant nextOpenAt,
        List<OpeningHourRangeDto> openingHours,
        List<MediaDto> images,
        @Schema(description = "Chỉ có giá trị khi người gọi đã đăng nhập")
        Boolean isFavorited,
        @Schema(description = "Số lượt đến của chính người gọi; chỉ có khi đã đăng nhập")
        Integer ownVisitCount,
        Instant createdAt
) {
}
