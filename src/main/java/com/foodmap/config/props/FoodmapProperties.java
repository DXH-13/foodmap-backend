package com.foodmap.config.props;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * Toàn bộ cấu hình riêng của FoodMap, gom vào một chỗ dưới tiền tố {@code foodmap.*}.
 *
 * <p>Gom theo nhóm thay vì rải {@code @Value} khắp nơi: dễ nhìn thấy hệ thống cần
 * những gì, và sai cấu hình thì lỗi ngay lúc khởi động chứ không phải lúc chạy.
 */
@Validated
@ConfigurationProperties(prefix = "foodmap")
public record FoodmapProperties(
        Jwt jwt,
        Cors cors,
        Geo geo,
        Storage storage,
        Chat chat
) {

    public record Jwt(
            @NotBlank String secret,
            @Min(1) int accessTokenTtlMinutes,
            @Min(1) int refreshTokenTtlDays,
            @NotBlank String issuer
    ) {}

    public record Cors(List<String> allowedOrigins) {}

    /**
     * Ràng buộc địa lý. Xem docs/04-data/geo-model.md.
     */
    public record Geo(
            @Min(1) int defaultRadiusMeters,
            @Min(1) int minRadiusMeters,
            @Min(1) int maxRadiusMeters,
            /* Bán kính tối đa cho phép ghi nhận một lượt đến (FR-VISIT-02). */
            @Min(1) int visitMaxDistanceMeters
    ) {}

    public record Storage(
            String endpoint,
            String region,
            String bucket,
            String accessKey,
            String secretKey,
            boolean pathStyleAccess
    ) {}

    public record Chat(
            String apiKey,
            String model,
            @Min(1) int maxMessagesPerHour
    ) {}
}
