package com.foodmap.notification.api;

/**
 * Loại thông báo. Khớp enum {@code NotificationType} trong hợp đồng và CHECK
 * trên bảng {@code notifications} — hai nơi này chỉ có đúng 6 giá trị dưới đây.
 *
 * <p>Hệ quả: chào mừng tài khoản mới và "quán có đánh giá mới" không có loại
 * riêng trong enum (không có {@code WELCOME} hay {@code NEW_REVIEW}) nên dùng
 * loại chung {@link #SYSTEM_ANNOUNCEMENT} và phân biệt bằng {@code title_key}
 * (xem {@code NotificationEventListener}). Muốn loại riêng thì phải đổi cả
 * hợp đồng lẫn CHECK ở migration mới — việc của bước sau, không tự ý thêm ở đây.
 */
public enum NotificationType {
    REVIEW_APPROVED,
    REVIEW_REJECTED,
    FEEDBACK_RESOLVED,
    NEW_PLACE_NEARBY,
    PLACE_UPDATED,
    SYSTEM_ANNOUNCEMENT
}
