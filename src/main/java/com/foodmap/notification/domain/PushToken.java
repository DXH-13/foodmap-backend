package com.foodmap.notification.domain;

import com.foodmap.notification.api.Platform;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Push token của một thiết bị mà người dùng đã đăng ký.
 *
 * <p>Không extends {@code BaseEntity}: bảng {@code push_tokens} không có cột
 * {@code updated_at}. Đăng ký lại cùng token thì chỉ cập nhật
 * {@code last_used_at} chứ không tạo bản ghi mới (xem
 * {@code NotificationService.registerPushToken}).
 */
@Entity
@Table(name = "push_tokens")
@EntityListeners(AuditingEntityListener.class)
public class PushToken {

    @Id
    @GeneratedValue
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 255)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Platform platform;

    @Column(name = "last_used_at", nullable = false)
    private Instant lastUsedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected PushToken() {
    }

    public PushToken(UUID userId, String token, Platform platform) {
        this.userId = userId;
        this.token = token;
        this.platform = platform;
        this.lastUsedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getToken() {
        return token;
    }

    public Platform getPlatform() {
        return platform;
    }

    public Instant getLastUsedAt() {
        return lastUsedAt;
    }

    /** Thiết bị vừa dùng lại token này — chạm mốc để biết token còn sống. */
    public void touch() {
        this.lastUsedAt = Instant.now();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
