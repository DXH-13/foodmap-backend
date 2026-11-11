package com.foodmap.identity;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Cấu hình JWT, thuộc về module {@code identity}.
 *
 * <p>Trước đây nhóm này nằm trong {@code FoodmapProperties.Jwt} ở
 * {@code config.props} — một package con nội bộ của module {@code config},
 * nên {@code JwtService} (module {@code identity}) dùng nó là vi phạm ranh giới
 * Modulith ("depends on non-exposed type"). Đưa về đây thì {@code identity} tự
 * sở hữu cấu hình của mình; tiền tố YAML giữ nguyên {@code foodmap.jwt.*}
 * nên {@code application.yml} không cần đổi.
 */
@Validated
@ConfigurationProperties(prefix = "foodmap.jwt")
public record JwtProperties(
        @NotBlank String secret,
        @Min(1) int accessTokenTtlMinutes,
        @Min(1) int refreshTokenTtlDays,
        @NotBlank String issuer
) {}
