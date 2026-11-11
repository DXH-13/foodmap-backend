package com.foodmap.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Refresh token, ánh xạ bảng {@code refresh_tokens} (V1).
 *
 * <p>Refresh token dùng <b>một lần</b> (xem openapi.yaml, operation {@code refreshToken}):
 * mỗi lần làm mới thì token cũ bị thu hồi và cấp token mới. Dùng lại token đã thu hồi
 * là dấu hiệu token bị đánh cắp — toàn bộ phiên của người dùng đó bị vô hiệu.
 */
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken extends AbstractToken {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    protected RefreshToken() {
    }

    public RefreshToken(User user, String tokenHash, Instant expiresAt) {
        super(tokenHash, expiresAt);
        this.user = user;
    }

    public void revoke() {
        if (revokedAt == null) {
            revokedAt = Instant.now();
        }
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isUsable() {
        return !isRevoked() && !isExpired();
    }

    public User getUser() {
        return user;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }
}
