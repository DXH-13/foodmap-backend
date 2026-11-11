package com.foodmap.identity.api;

/**
 * Vai trò của người dùng, khớp CHECK constraint {@code users_role_check} trong V1
 * và enum {@code UserRole} trong docs/SDD/api/openapi.yaml.
 */
public enum UserRole {
    USER,
    MODERATOR,
    ADMIN
}
