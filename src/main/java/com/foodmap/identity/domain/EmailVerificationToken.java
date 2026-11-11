package com.foodmap.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Token xác minh email, ánh xạ bảng {@code email_verification_tokens} (V1).
 * Dùng một lần: sau khi dùng thì đánh dấu {@code used_at}, dùng lại sẽ bị từ chối.
 */
@Entity
@Table(name = "email_verification_tokens")
public class EmailVerificationToken extends AbstractToken {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @Column(name = "used_at")
    private Instant usedAt;

    protected EmailVerificationToken() {
    }

    public EmailVerificationToken(User user, String tokenHash, Instant expiresAt) {
        super(tokenHash, expiresAt);
        this.user = user;
    }

    public void markUsed() {
        if (usedAt == null) {
            usedAt = Instant.now();
        }
    }

    public boolean isUsed() {
        return usedAt != null;
    }

    public boolean isUsable() {
        return !isUsed() && !isExpired();
    }

    public User getUser() {
        return user;
    }

    public Instant getUsedAt() {
        return usedAt;
    }
}
