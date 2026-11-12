package com.foodmap.notification.api;

/**
 * Nền tảng của thiết bị đăng ký push token. Khớp enum {@code platform} trong
 * {@code RegisterPushTokenRequest} và CHECK trên bảng {@code push_tokens}.
 *
 * <p>Đặt ở {@code api/} vì xuất hiện trong JSON request/response — một phần của
 * hợp đồng công khai, không phải chi tiết nội bộ (xem tiền lệ {@code UserRole}).
 */
public enum Platform {
    IOS,
    ANDROID
}
