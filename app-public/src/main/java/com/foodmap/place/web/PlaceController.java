package com.foodmap.place.web;

import com.foodmap.common.PageResponse;
import com.foodmap.place.PlaceService;
import com.foodmap.place.dto.PlaceSummaryDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

            @RequestParam(defaultValue = "0") @Min(0) int page,

            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {

        return placeService.searchNearby(latitude, longitude, radiusMeters, PageRequest.of(page, size));
    }
}
