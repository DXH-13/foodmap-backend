package com.foodmap.favorite.web;

import com.foodmap.auth.AuthPrincipal;
import com.foodmap.common.PageResponse;
import com.foodmap.favorite.FavoriteService;
import com.foodmap.place.PlaceService;
import com.foodmap.place.dto.PlaceSummaryDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Danh sách yêu thích. Đọc qua {@link PlaceService} vì kết quả là {@code PlaceSummary} —
 * yêu thích chỉ là bộ lọc trên địa điểm.
 */
@RestController
@Tag(name = "favorite", description = "Danh sách yêu thích")
public class FavoriteController {

    private final PlaceService placeService;
    private final FavoriteService favoriteService;

    public FavoriteController(PlaceService placeService, FavoriteService favoriteService) {
        this.placeService = placeService;
        this.favoriteService = favoriteService;
    }

    @PutMapping("/api/v1/me/favorites/{placeId}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(
            operationId = "addFavorite",
            summary = "Thêm một địa điểm vào danh sách yêu thích",
            description = "Idempotent — gọi lại với địa điểm đã yêu thích vẫn trả 200.")
    public void addFavorite(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable UUID placeId) {
        favoriteService.add(principal.userId(), placeId);
    }

    @DeleteMapping("/api/v1/me/favorites/{placeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(operationId = "removeFavorite", summary = "Bỏ một địa điểm khỏi danh sách yêu thích")
    public void removeFavorite(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable UUID placeId) {
        favoriteService.remove(principal.userId(), placeId);
    }

    @GetMapping("/api/v1/me/favorites")
    @Operation(
            operationId = "listFavorites",
            summary = "Lấy danh sách yêu thích",
            description = "sort = DISTANCE bắt buộc có latitude và longitude, thiếu thì 400 COORDINATES_REQUIRED.")
    public PageResponse<PlaceSummaryDto> listFavorites(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestParam(defaultValue = "RECENTLY_ADDED") PlaceService.FavoriteSort sort,
            @Parameter(description = "Bắt buộc khi sort = DISTANCE")
            @RequestParam(required = false) Double latitude,
            @Parameter(description = "Bắt buộc khi sort = DISTANCE")
            @RequestParam(required = false) Double longitude,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return placeService.listFavorites(principal.userId(), sort, latitude, longitude, PageRequest.of(page, size));
    }
}
