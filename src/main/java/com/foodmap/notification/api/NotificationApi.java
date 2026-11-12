package com.foodmap.notification.api;

import java.util.Map;
import java.util.UUID;

/**
 * Giao diện công khai của module {@code notification} cho module khác nhờ gửi
 * thông báo cho một người dùng.
 *
 * <p>Module gọi chỉ nói "nhắc người này, loại này, nội dung này" — việc có gửi
 * hay không (theo cài đặt của người nhận), gửi trong app hay đẩy push, là quyết
 * định riêng của {@code notification}. Bước 5 chưa có module nào gọi trực tiếp
 * (các thông báo hiện nay đều đi qua listener sự kiện); giao diện này để sẵn
 * cho các module sau (ví dụ {@code moderation}, {@code content}).
 */
public interface NotificationApi {

    /**
     * Tạo thông báo trong app cho người dùng, rồi thử đẩy push nếu người đó có
     * đăng ký token và loại này được phép trong cài đặt của họ.
     *
     * <p>Không làm gì (im lặng) khi người dùng đã tắt loại thông báo này trong
     * cài đặt — đó là ý muốn của họ, không phải lỗi. Không bao giờ ném lỗi vì
     * gửi push thất bại: thông báo trong app đã tạo thì vẫn còn đó.
     *
     * @param titleKey key i18n của tiêu đề (ví dụ {@code notification.welcome.title}),
     *                 tiêu đề hiển thị được dịch lúc đọc theo ngôn ngữ của người nhận.
     * @param payload dữ liệu kèm theo, lưu nguyên dạng JSONB — thường gồm
     *                {@code placeId} / {@code reviewId} để app mở đúng màn hình.
     */
    void notify(UUID userId, NotificationType type, String titleKey, Map<String, Object> payload);
}
