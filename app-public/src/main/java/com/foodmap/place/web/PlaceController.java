package com.foodmap.place.web;

import com.foodmap.auth.AuthPrincipal;
import com.foodmap.common.PageResponse;
import com.foodmap.place.PlaceService;
import com.foodmap.place.PlaceType;
import com.foodmap.place.dto.PlaceDetailDto;
import com.foodmap.place.dto.PlaceSummaryDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/places")
@Tag(name = "place", description = "Địa điểm và tìm kiếm trên bản đồ")
public class PlaceController {

    private final PlaceService placeService;

    public PlaceController(PlaceService placeService) {
        this.placeService = placeService;
    }

    @GetMapping("/nearby")
    @Operation(
            operationId = "searchNearbyPlaces",
            summary = "Tìm địa điểm quanh một toạ độ",
            description = """
                    Trả về địa điểm trong bán kính cho trước, sắp xếp theo khoảng cách tăng dần.
                    Bán kính vượt giới hạn trả về 400 RADIUS_OUT_OF_RANGE, không âm thầm cắt bớt.
                    Không cần đăng nhập.""")
    public PageResponse<PlaceSummaryDto> searchNearbyPlaces(
            @Parameter(description = "Vĩ độ của điểm gốc", example = "10.8231")
            @RequestParam @Min(-90) @Max(90) double latitude,

            @Parameter(description = "Kinh độ của điểm gốc", example = "106.6297")
            @RequestParam @Min(-180) @Max(180) double longitude,

            @Parameter(description = "Bán kính tìm kiếm, đơn vị mét")
            @RequestParam(required = false) Integer radiusMeters,

            @Parameter(description = "Lọc theo loại địa điểm")
            @RequestParam(required = false) PlaceType placeType,

            @Parameter(description = "Lọc theo một hoặc nhiều danh mục")
            @RequestParam(required = false) List<String> categorySlugs,

            @Parameter(description = "Điểm trung bình tối thiểu")
            @RequestParam(required = false) @DecimalMin("1") @DecimalMax("5") Double minRating,

            @Parameter(description = "Chỉ lấy địa điểm đang mở cửa tại thời điểm truy vấn")
            @RequestParam(defaultValue = "false") boolean openNow,

            @RequestParam(defaultValue = "0") @Min(0) int page,

            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {

        return placeService.searchNearby(latitude, longitude, radiusMeters,
                new PlaceService.NearbyFilter(placeType, categorySlugs, minRating, openNow),
                PageRequest.of(page, size));
    }

    @GetMapping("/{placeId}")
    @Operation(
            operationId = "getPlaceById",
            summary = "Lấy chi tiết một địa điểm",
            description = "Không cần đăng nhập. Có token thì điền thêm isFavorited và ownVisitCount.")
    public PlaceDetailDto getPlaceById(@PathVariable UUID placeId,
                                       @AuthenticationPrincipal AuthPrincipal principal) {
        return placeService.getDetail(placeId, principal == null ? null : principal.userId());
    }

    @GetMapping
    @Operation(
            operationId = "searchPlaces",
            summary = "Tìm địa điểm theo từ khoá",
            description = "Tìm trên tên và mô tả, không phân biệt hoa thường và không phân biệt dấu.")
    public PageResponse<PlaceSummaryDto> searchPlaces(
            @Parameter(description = "Từ khoá tìm kiếm")
            @RequestParam(required = false) @Size(min = 1, max = 100) String query,

            @RequestParam(required = false) PlaceType placeType,

            @RequestParam(required = false) List<String> categorySlugs,

            @RequestParam(defaultValue = "RELEVANCE") PlaceService.SearchSort sort,

            @RequestParam(defaultValue = "0") @Min(0) int page,

            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {

        return placeService.search(query, placeType, categorySlugs, sort, PageRequest.of(page, size));
    }
}
