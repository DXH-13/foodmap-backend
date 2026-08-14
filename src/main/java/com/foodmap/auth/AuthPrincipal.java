package com.foodmap.auth;

import java.util.UUID;

/**
 * Danh tính người gọi, lấy từ access token.
 *
 * <p>{@code emailVerified} nằm trong token để không phải truy vấn CSDL ở mỗi request.
 * Đánh đổi: người dùng vừa xác minh email phải đợi token làm mới (tối đa 15 phút)
 * mới dùng được các tính năng cần xác minh. Chấp nhận được, và client có thể chủ động
 * làm mới token ngay sau khi xác minh.
 *
 * @param userId        id người dùng
 * @param role          {@code USER} | {@code MODERATOR} | {@code ADMIN}
 * @param emailVerified đã xác minh email chưa (FR-AUTH-02)
 */
public record AuthPrincipal(UUID userId, String role, boolean emailVerified) {
}
