package com.foodmap.notification.infrastructure;

import com.foodmap.notification.domain.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/** Truy vấn bảng {@code notifications} — chỉ module {@code notification} dùng. */
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Page<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Page<Notification> findByUserIdAndReadFalseOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    long countByUserIdAndReadFalse(UUID userId);

    /**
     * Đánh dấu đã đọc toàn bộ thông báo của người dùng.
     * Chỉ chạm những hàng chưa đọc — tránh ghi đè vô ích.
     */
    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.userId = :userId AND n.read = false")
    int markAllRead(@Param("userId") UUID userId);

    /** Đánh dấu đã đọc một số thông báo — chỉ những cái thuộc về người dùng này. */
    @Modifying
    @Query("UPDATE Notification n SET n.read = true "
            + "WHERE n.userId = :userId AND n.id IN :ids AND n.read = false")
    int markReadByIds(@Param("userId") UUID userId, @Param("ids") List<UUID> ids);
}
