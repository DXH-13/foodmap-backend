package com.foodmap.notification.domain;

import com.foodmap.notification.api.NotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Cài đặt nhận thông báo của một người dùng: danh sách các loại được phép gửi.
 *
 * <p>Khoá chính chính là {@code user_id} (mỗi người chỉ có một hàng). Bảng này
 * không có {@code created_at} nên tự khai trường, chỉ có {@code updated_at}
 * để kiểm toán. Không có hàng nghĩa là chưa từng đặt — mặc định cho phép tất
 * cả các loại (xem {@code NotificationService.isTypeEnabled}).
 */
@Entity
@Table(name = "notification_settings")
@EntityListeners(AuditingEntityListener.class)
public class NotificationSettings {

    @Id
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "enabled_types", columnDefinition = "jsonb", nullable = false)
    private List<NotificationType> enabledTypes;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected NotificationSettings() {
    }

    public NotificationSettings(UUID userId, List<NotificationType> enabledTypes) {
        this.userId = userId;
        this.enabledTypes = new ArrayList<>(enabledTypes);
    }

    public UUID getUserId() {
        return userId;
    }

    public List<NotificationType> getEnabledTypes() {
        return List.copyOf(enabledTypes);
    }

    public void setEnabledTypes(List<NotificationType> enabledTypes) {
        this.enabledTypes = new ArrayList<>(enabledTypes);
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
