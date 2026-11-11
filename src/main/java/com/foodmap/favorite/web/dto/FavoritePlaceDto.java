package com.foodmap.favorite.web.dto;

import com.foodmap.place.api.PlaceSummary;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Một địa điểm trong danh sách yêu thích. Khớp schema {@code PlaceSummary}
 * trong hợp đồng.
 *
 * <p>{@code distanceMeters} chỉ có giá trị khi danh sách được sắp xếp theo
 * khoảng cách (sort = DISTANCE); các trường hợp khác là {@code null}.
 */
public record FavoritePlaceDto(
        UUID id,
        String slug,
        String name,
        String placeType,
        CoordinatesDto coordinates,
        String address,
        String status,
        BigDecimal averageRating,
        int reviewCount,
        long visitCount,
        Double distanceMeters
) {

    public static FavoritePlaceDto from(PlaceSummary place, Double distanceMeters) {
        return new FavoritePlaceDto(
                place.id(),
                place.slug(),
                place.name(),
                place.placeType(),
                new CoordinatesDto(place.latitude(), place.longitude()),
                place.address(),
                place.status(),
                place.averageRating(),
                place.reviewCount(),
                place.visitCount(),
                distanceMeters);
    }
}
