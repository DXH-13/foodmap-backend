package com.foodmap.identity.api;

import java.util.UUID;

/**
 * Tài khoản vừa được đăng ký thành công.
 *
 * <p>Người nghe hiện tại: {@code notification} ở Bước 5 gửi thông báo chào mừng
 * trong app. Sự kiện được phát trong cùng transaction với việc tạo tài khoản
 * (xem {@code AuthService.register}) nên outbox ghi lại và listener chạy sau
 * commit — đăng ký không bao giờ mất thông báo chào mừng vì listener lỗi.
 */
public record UserRegistered(UUID userId, String email) {
}
