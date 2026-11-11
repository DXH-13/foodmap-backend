package com.foodmap.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;

import java.time.Instant;
import java.util.UUID;

/**
 * Phần chung của các token có trạng thái (refresh, đặt lại mật khẩu, xác minh email).
 *
 * <p>Không dùng {@link com.foodmap.common.BaseEntity} vì các bảng token trong V1
 * không có cột {@code updated_at} — map thừa cột sẽ làm Hibernate validate lỗi.
 */
@MappedSuperclass
public abstract class AbstractToken {

    @Id
    @GeneratedValue
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "token_hash", nullable = false, length = 64, updatable = false)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AbstractToken() {
    }

    protected AbstractToken(String tokenHash, Instant expiresAt) {
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public UUID getId() {
        return id;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
