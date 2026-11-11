package com.foodmap.config.props;

import jakarta.validation.constraints.Min;
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
        Cors cors,
        Storage storage,
        Chat chat
) {

    public record Cors(List<String> allowedOrigins) {}

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
