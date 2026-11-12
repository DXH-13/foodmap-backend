package com.foodmap.notification.application;

import com.foodmap.notification.api.NotificationType;

import java.util.Map;
import java.util.UUID;

/**
 * Cổng gửi push tới thiết bị của người dùng (FCM/APNs).
 *
 * <p>Code nghiệp vụ chỉ biết giao diện này — không biết nhà cung cấp push là ai,
 * không cầm credential. Bản thật (FCM) thay vào sau khi có service account của
 * Firebase; hiện tại chỉ có bản stub ghi log (xem {@code LoggingPushSender}).
 *
 * <p><b>Hợp đồng của cổng:</b> không bao giờ ném lỗi — gửi không được thì trả
 * {@code false} để bên gọi ghi nhận và tiếp tục; thông báo trong app đã tạo
 * thì vẫn còn đó, người dùng mở app vẫn thấy.
 */
public interface PushSender {

    /**
     * Đẩy một thông báo tới mọi thiết bị đã đăng ký của người dùng.
     *
     * @return {@code true} khi ít nhất một thiết bị đã nhận được (để bên gọi
     *         đánh dấu {@code push_sent_at}); {@code false} khi chưa gửi được
     *         cho thiết bị nào — kể cả khi chưa có nhà cung cấp push thật.
     */
    boolean sendPush(UUID userId, NotificationType type, String title, Map<String, Object> payload);
}
