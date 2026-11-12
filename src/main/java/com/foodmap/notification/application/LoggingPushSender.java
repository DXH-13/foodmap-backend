package com.foodmap.notification.application;

import com.foodmap.notification.api.NotificationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Bản stub của {@link PushSender}: chỉ ghi log, không gửi đi đâu cả.
 *
 * <p>Lý do: chưa có credential FCM/APNs (cần service account của Firebase do
 * người quản trị dự án cấp — không tự bịa, không commit key vào repo). Khi có
 * credential, thay bean này bằng bản dùng Firebase Admin SDK; code nghiệp vụ
 * và các test không đổi vì chỉ phụ thuộc vào giao diện {@code PushSender}.
 *
 * <p>Luôn trả {@code false} nên {@code push_sent_at} của thông báo giữ
 * {@code null} — đó là sự thật: chưa có push nào được gửi thật.
 */
@Component
public class LoggingPushSender implements PushSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingPushSender.class);

    @Override
    public boolean sendPush(UUID userId, NotificationType type, String title,
                            Map<String, Object> payload) {
        log.info("Push chưa cấu hình (stub): userId={} type={} title={} payload={} — "
                + "thông báo trong app vẫn được tạo, push_sent_at giữ null.",
                userId, type, title, payload);
        return false;
    }
}
