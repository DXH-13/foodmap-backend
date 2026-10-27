package com.foodmap.user.dto;

import com.foodmap.user.User;
import com.foodmap.user.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/** Khớp schema {@code UserProfile} trong docs/SDD/api/openapi.yaml. */
@Schema(description = "Hồ sơ người dùng")
public record UserProfileDto(
        UUID id,
        String email,
        String displayName,
        String avatarUrl,
        UserRole role,
        @Schema(allowableValues = {"vi", "en"})
        String preferredLocale,
        boolean emailVerified,
        Instant createdAt
) {

    public static UserProfileDto from(User user) {
        return new UserProfileDto(
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
