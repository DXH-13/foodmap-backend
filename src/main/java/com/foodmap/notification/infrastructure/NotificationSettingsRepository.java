package com.foodmap.notification.infrastructure;

import com.foodmap.notification.domain.NotificationSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** Truy vấn bảng {@code notification_settings} — chỉ module {@code notification} dùng. */
public interface NotificationSettingsRepository extends JpaRepository<NotificationSettings, UUID> {
}
