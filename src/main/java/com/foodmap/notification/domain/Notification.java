package com.foodmap.notification.domain;

import com.foodmap.notification.api.NotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Một thông báo trong app của một người dùng.
 *
 * <p>Không extends {@code BaseEntity}: bảng {@code notifications} không có cột
 * {@code updated_at} nên entity này tự khai {@code id} và {@code created_at}
 * (xem tiền lệ {@code Favorite} ở Bước 4). Chỉ tham chiếu tới người dùng và
 * địa điểm/đánh giá bằng mã định danh — không join sang bảng module khác.
 *
 * <p>Tiêu đề KHÔNG lưu chuỗi đã dịch: chỉ lưu {@code titleKey} + tham số trong
 * {@code payload}, tiêu đề hiển thị được dịch lúc đọc theo ngôn ngữ ưa thích
 * của người nhận (xem {@code NotificationService}).
 */
@Entity
@Table(name = "notifications", schema = "notification")
@EntityListeners(AuditingEntityListener.class)
public class Notification {

    @Id
    @GeneratedValue
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private NotificationType type;

    @Column(name = "title_key", nullable = false, length = 100)
    private String titleKey;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> payload;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(name = "push_sent_at")
    private Instant pushSentAt;

    @Column(name = "push_scheduled_at")
    private Instant pushScheduledAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Notification() {
    }

    public Notification(UUID userId, NotificationType type, String titleKey,
                        Map<String, Object> payload) {
        this.userId = userId;
        this.type = type;
        this.titleKey = titleKey;
        this.payload = payload == null ? Map.of() : Map.copyOf(payload);
        this.read = false;
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

    public NotificationType getType() {
        return type;
    }

    public String getTitleKey() {
        return titleKey;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public boolean isRead() {
        return read;
    }

    public void markRead() {
        this.read = true;
    }

    public Instant getPushSentAt() {
        return pushSentAt;
    }

    public void markPushSent(Instant sentAt) {
        this.pushSentAt = sentAt;
    }

    public Instant getPushScheduledAt() {
        return pushScheduledAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
