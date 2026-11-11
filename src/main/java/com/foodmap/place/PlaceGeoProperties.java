package com.foodmap.place;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Ràng buộc địa lý của module {@code place}. Xem docs/SDD/du-lieu/geo-model.md.
 *
 * <p>Trước đây nhóm này nằm trong {@code FoodmapProperties.Geo} ở
 * {@code config.props} — một package con nội bộ của module {@code config},
 * nên {@code PlaceService} (module {@code place}) dùng nó là vi phạm ranh
 * giới Modulith ("depends on non-exposed type"). Đưa về đây thì {@code place}
 * tự sở hữu cấu hình của mình; tiền tố YAML giữ nguyên {@code foodmap.geo.*}
 * nên {@code application.yml} không cần đổi.
 */
@Validated
@ConfigurationProperties(prefix = "foodmap.geo")
public record PlaceGeoProperties(
        @Min(1) int defaultRadiusMeters,
        @Min(1) int minRadiusMeters,
        @Min(1) int maxRadiusMeters,
        /* Bán kính tối đa cho phép ghi nhận một lượt đến (FR-VISIT-02). */
        @Min(1) int visitMaxDistanceMeters
) {}
