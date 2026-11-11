package com.foodmap.favorite.application;

import com.foodmap.common.BadRequestException;
import com.foodmap.common.GeoUtils;
import com.foodmap.common.NotFoundException;
import com.foodmap.common.PageResponse;
import com.foodmap.favorite.domain.Favorite;
import com.foodmap.favorite.infrastructure.FavoriteRepository;
import com.foodmap.favorite.web.dto.FavoritePlaceDto;
import com.foodmap.place.api.PlaceApi;
import com.foodmap.place.api.PlaceSummary;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Use case yêu thích (Phase 3): thêm (idempotent), bỏ và liệt kê.
 *
 * <p>Yêu thích chỉ là quan hệ giữa người dùng và địa điểm — module này không
 * phát sự kiện (không có module nào cần biết khi một địa điểm được yêu thích,
 * cho tới khi gợi ý ở phase sau cần).
 */
@Service
public class FavoriteService {

    private final FavoriteRepository favorites;
    private final PlaceApi places;

    public FavoriteService(FavoriteRepository favorites, PlaceApi places) {
        this.favorites = favorites;
        this.places = places;
    }

    /**
     * Thêm một địa điểm vào danh sách yêu thích. Idempotent — gọi lại với địa
     * điểm đã yêu thích vẫn trả 200 và không tạo bản ghi mới (unique index trên
     * bảng cũng chặn trùng ở tầng CSDL).
     */
    @Transactional
    public FavoritePlaceDto addFavorite(UUID userId, UUID placeId) {
        String locale = LocaleContextHolder.getLocale().getLanguage();
        PlaceSummary place = places.getSummaries(List.of(placeId), locale).stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("PLACE_NOT_FOUND", "place.error.not_found"));
        if (!favorites.existsByUserIdAndPlaceId(userId, placeId)) {
            favorites.save(new Favorite(userId, placeId));
        }
        return FavoritePlaceDto.from(place, null);
    }

    /**
     * Bỏ một địa điểm khỏi danh sách yêu thích. Luôn trả 204 kể cả khi địa điểm
     * vốn không nằm trong danh sách — hợp đồng không định nghĩa 404 cho trường
     * hợp này.
     */
    @Transactional
    public void removeFavorite(UUID userId, UUID placeId) {
        favorites.deleteByUserIdAndPlaceId(userId, placeId);
    }

    /**
     * Danh sách yêu thích của chính mình.
     *
     * <p>{@code sort = DISTANCE} bắt buộc kèm {@code latitude}/{@code longitude}
     * (thiếu thì 400) và sắp xếp bằng công thức Haversine trên toạ độ — đủ chính
     * xác cho việc sắp xếp, và số lượng yêu thích của một người là nhỏ nên tính
     * trong bộ nhớ rồi mới phân trang. {@code RECENTLY_ADDED} (mặc định) phân
     * trang ở tầng CSDL theo {@code created_at}.
     */
    @Transactional(readOnly = true)
    public PageResponse<FavoritePlaceDto> listFavorites(UUID userId, String sort,
                                                       Double latitude, Double longitude,
                                                       int page, int size) {
        requireValidPage(page, size);
        String locale = LocaleContextHolder.getLocale().getLanguage();
        if ("DISTANCE".equals(sort)) {
            return listByDistance(userId, latitude, longitude, page, size, locale);
        }
        if (sort != null && !"RECENTLY_ADDED".equals(sort)) {
            throw new BadRequestException("VALIDATION_FAILED", "common.error.validation");
        }
        var pageResult = favorites.findByUserIdOrderByCreatedAtDesc(
                userId, PageRequest.of(page, size));
        List<UUID> placeIds = pageResult.getContent().stream()
                .map(Favorite::getPlaceId).toList();
        Map<UUID, PlaceSummary> byId = summariesById(placeIds, locale);
        List<FavoritePlaceDto> content = pageResult.getContent().stream()
                .map(favorite -> byId.get(favorite.getPlaceId()))
                .filter(summary -> summary != null)
                .map(summary -> FavoritePlaceDto.from(summary, null))
                .toList();
        return new PageResponse<>(content, pageResult.getNumber(), pageResult.getSize(),
                pageResult.getTotalElements(), pageResult.getTotalPages());
    }

    private PageResponse<FavoritePlaceDto> listByDistance(UUID userId, Double latitude,
                                                          Double longitude, int page, int size,
                                                          String locale) {
        if (latitude == null || longitude == null
                || latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new BadRequestException("VALIDATION_FAILED", "common.error.validation");
        }
        List<Favorite> all = favorites.findByUserIdOrderByCreatedAtDesc(userId);
        List<UUID> placeIds = all.stream().map(Favorite::getPlaceId).toList();
        Map<UUID, PlaceSummary> byId = summariesById(placeIds, locale);
        List<FavoritePlaceDto> sorted = all.stream()
                .map(favorite -> byId.get(favorite.getPlaceId()))
                .filter(summary -> summary != null)
                .map(summary -> FavoritePlaceDto.from(summary,
                        GeoUtils.distanceMeters(latitude, longitude,
                                summary.latitude(), summary.longitude())))
                .sorted(Comparator.comparingDouble(FavoritePlaceDto::distanceMeters))
                .toList();
        int from = Math.min(page * size, sorted.size());
        int to = Math.min(from + size, sorted.size());
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) sorted.size() / size);
        return new PageResponse<>(sorted.subList(from, to), page, size, sorted.size(), totalPages);
    }

    private Map<UUID, PlaceSummary> summariesById(List<UUID> placeIds, String locale) {
        return places.getSummaries(placeIds, locale).stream()
                .collect(Collectors.toMap(PlaceSummary::id, Function.identity()));
    }

    private void requireValidPage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new BadRequestException("VALIDATION_FAILED", "common.error.validation");
        }
    }
}
