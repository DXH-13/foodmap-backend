package com.foodmap.identity.api;

import com.foodmap.identity.domain.User;

import java.time.Instant;
import java.util.UUID;

/**
 * Hồ sơ người dùng trả ra API, khớp schema {@code UserProfile} trong
 * docs/SDD/api/openapi.yaml.
 *
 * <p>Đây là hình dạng duy nhất module khác được thấy về người dùng — không bao giờ
 * lộ {@code passwordHash} hay token.
 */
public record UserProfile(
        UUID id,
        String email,
        String displayName,
        String avatarUrl,
        UserRole role,
        String preferredLocale,
        boolean emailVerified,
        Instant createdAt
) {

    public static UserProfile from(User user) {
        return new UserProfile(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                user.getRole(),
                user.getPreferredLocale(),
                user.isEmailVerified(),
                user.getCreatedAt());
    }
}
