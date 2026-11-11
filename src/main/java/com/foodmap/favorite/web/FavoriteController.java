package com.foodmap.favorite.web;

import com.foodmap.common.PageResponse;
import com.foodmap.favorite.application.FavoriteService;
import com.foodmap.favorite.web.dto.FavoritePlaceDto;
import com.foodmap.identity.api.AuthPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Yêu thích — của chính mình. Khớp các operation {@code listFavorites},
 * {@code addFavorite}, {@code removeFavorite} trong hợp đồng.
 */
@RestController
@RequestMapping("/app/v1/me/favorites")
@Tag(name = "favorite", description = "Địa điểm yêu thích")
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @GetMapping
    @Operation(operationId = "listFavorites", summary = "Lấy danh sách yêu thích")
    public PageResponse<FavoritePlaceDto> listFavorites(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestParam(defaultValue = "RECENTLY_ADDED") String sort,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return favoriteService.listFavorites(
                principal.userId(), sort, latitude, longitude, page, size);
    }

    @PutMapping("/{placeId}")
    @Operation(operationId = "addFavorite", summary = "Thêm một địa điểm vào danh sách yêu thích")
    public FavoritePlaceDto addFavorite(@PathVariable UUID placeId,
                                        @AuthenticationPrincipal AuthPrincipal principal) {
        return favoriteService.addFavorite(principal.userId(), placeId);
    }

    @DeleteMapping("/{placeId}")
    @Operation(operationId = "removeFavorite", summary = "Bỏ một địa điểm khỏi danh sách yêu thích")
    public ResponseEntity<Void> removeFavorite(@PathVariable UUID placeId,
                                               @AuthenticationPrincipal AuthPrincipal principal) {
        favoriteService.removeFavorite(principal.userId(), placeId);
        return ResponseEntity.noContent().build();
    }
}
