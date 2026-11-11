package com.foodmap.identity.domain;

import com.foodmap.common.BaseEntity;
import com.foodmap.identity.api.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Tài khoản người dùng, ánh xạ bảng {@code users} (V1).
 *
 * <p>Email lưu ở dạng chữ thường để tra cứu không phân biệt hoa thường khớp với
 * unique index {@code uq_users_email} trên {@code LOWER(email)}. Mật khẩu chỉ lưu
 * dạng hash BCrypt (60 ký tự) — không bao giờ lưu mật khẩu gốc.
 */
@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Column(nullable = false, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 60)
    private String passwordHash;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private UserRole role = UserRole.USER;

    @Column(name = "preferred_locale", nullable = false, length = 5)
    private String preferredLocale = "vi";

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected User() {
    }

    private User(String email, String passwordHash, String displayName, String preferredLocale) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.preferredLocale = preferredLocale;
        this.role = UserRole.USER;
        this.emailVerified = false;
    }

    public static User register(String email, String passwordHash, String displayName, String preferredLocale) {
        return new User(email, passwordHash, displayName, preferredLocale);
    }

    public void markEmailVerified() {
        this.emailVerified = true;
        this.emailVerifiedAt = Instant.now();
    }

    public void changePassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
    }

    public void changeRole(UserRole newRole) {
        this.role = newRole;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public UserRole getRole() {
        return role;
    }

    public String getPreferredLocale() {
        return preferredLocale;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public Instant getEmailVerifiedAt() {
        return emailVerifiedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }
}
