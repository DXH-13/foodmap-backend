package com.foodmap.favorite;

import com.foodmap.common.NotFoundException;
import com.foodmap.favorite.application.FavoriteService;
import com.foodmap.favorite.domain.Favorite;
import com.foodmap.favorite.infrastructure.FavoriteRepository;
import com.foodmap.place.api.PlaceApi;
import com.foodmap.place.api.PlaceSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Test use case yêu thích ở tầng service, không cần CSDL hay Docker. */
class FavoriteServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID PLACE_A = UUID.randomUUID();
    private static final UUID PLACE_B = UUID.randomUUID();

    private FavoriteRepository favorites;
    private PlaceApi places;
    private FavoriteService favoriteService;

    private static PlaceSummary summary(UUID id, String name, double lat, double lng) {
        return new PlaceSummary(id, "slug-" + id.toString().substring(0, 8), name, "RESTAURANT",
                lat, lng, "Địa chỉ", "PUBLISHED", new BigDecimal("4.5"), 3, 10);
    }

    @BeforeEach
    void setUp() {
        favorites = mock(FavoriteRepository.class);
        places = mock(PlaceApi.class);
        favoriteService = new FavoriteService(favorites, places);
        when(places.getSummaries(eq(List.of(PLACE_A)), anyString()))
                .thenReturn(List.of(summary(PLACE_A, "Quán A", 10.0, 106.0)));
        when(places.getSummaries(eq(List.of(PLACE_B)), anyString())).thenReturn(List.of());
    }

    @Test
    @DisplayName("Thêm mới: lưu bản ghi và trả thông tin địa điểm")
    void addFavoriteSaves() {
        when(favorites.existsByUserIdAndPlaceId(USER_ID, PLACE_A)).thenReturn(false);

        var dto = favoriteService.addFavorite(USER_ID, PLACE_A);

        assertThat(dto.id()).isEqualTo(PLACE_A);
        assertThat(dto.name()).isEqualTo("Quán A");
        verify(favorites).save(any(Favorite.class));
    }

    @Test
    @DisplayName("Thêm lại địa điểm đã yêu thích: idempotent, không lưu thêm")
    void addFavoriteIsIdempotent() {
        when(favorites.existsByUserIdAndPlaceId(USER_ID, PLACE_A)).thenReturn(true);

        var dto = favoriteService.addFavorite(USER_ID, PLACE_A);

        assertThat(dto.id()).isEqualTo(PLACE_A);
        verify(favorites, never()).save(any(Favorite.class));
    }

    @Test
    @DisplayName("Thêm địa điểm không tồn tại: 404 PLACE_NOT_FOUND")
    void addFavoriteRequiresExistingPlace() {
        assertThatThrownBy(() -> favoriteService.addFavorite(USER_ID, PLACE_B))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("PLACE_NOT_FOUND");
    }

    @Test
    @DisplayName("Bỏ yêu thích: gọi xoá theo user + place")
    void removeFavoriteDeletes() {
        favoriteService.removeFavorite(USER_ID, PLACE_A);

        verify(favorites).deleteByUserIdAndPlaceId(USER_ID, PLACE_A);
    }

    @Test
    @DisplayName("Sắp xếp DISTANCE thiếu toạ độ: 400")
    void distanceSortRequiresCoordinates() {
        assertThatThrownBy(() -> favoriteService.listFavorites(
                USER_ID, "DISTANCE", null, null, 0, 20))
                .hasMessage("VALIDATION_FAILED");
    }

    @Test
    @DisplayName("Sắp xếp DISTANCE: quán gần hơn đứng trước, kèm khoảng cách")
    void distanceSortOrdersByDistance() {
        Favorite favA = new Favorite(USER_ID, PLACE_A);
        Favorite favB = new Favorite(USER_ID, PLACE_B);
        when(favorites.findByUserIdOrderByCreatedAtDesc(USER_ID)).thenReturn(List.of(favB, favA));
        when(places.getSummaries(eq(List.of(PLACE_B, PLACE_A)), anyString())).thenReturn(List.of(
                summary(PLACE_B, "Quán B", 10.1, 106.0),
                summary(PLACE_A, "Quán A", 10.0, 106.0)));

        var page = favoriteService.listFavorites(USER_ID, "DISTANCE", 10.0, 106.0, 0, 20);

        assertThat(page.content()).hasSize(2);
        assertThat(page.content().get(0).id()).isEqualTo(PLACE_A);
        assertThat(page.content().get(0).distanceMeters()).isEqualTo(0.0);
        assertThat(page.content().get(1).id()).isEqualTo(PLACE_B);
        assertThat(page.content().get(1).distanceMeters()).isGreaterThan(10_000.0);
    }

    @Test
    @DisplayName("Mặc định RECENTLY_ADDED: giữ thứ tự mới nhất trước, không có khoảng cách")
    void defaultSortKeepsRecencyOrder() {
        Favorite favA = new Favorite(USER_ID, PLACE_A);
        when(favorites.findByUserIdOrderByCreatedAtDesc(USER_ID, PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(favA), PageRequest.of(0, 20), 1));
        when(places.getSummaries(eq(List.of(PLACE_A)), anyString()))
                .thenReturn(List.of(summary(PLACE_A, "Quán A", 10.0, 106.0)));

        var page = favoriteService.listFavorites(USER_ID, "RECENTLY_ADDED", null, null, 0, 20);

        assertThat(page.content()).hasSize(1);
        assertThat(page.content().get(0).distanceMeters()).isNull();
        assertThat(page.totalElements()).isEqualTo(1);
    }
}
