package com.foodmap.identity.api;

import com.foodmap.common.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Giao diện công khai của module {@code identity} cho module khác gọi qua {@code api/}.
 *
 * <p>Module khác chỉ được dùng những gì ở đây — không import thẳng vào {@code domain/},
 * {@code application/}, {@code web/} hay {@code infrastructure/} của {@code identity}.
 * Đây là mối nối cho module {@code user} (hồ sơ của chính mình, quản trị người dùng)
 * và cho các module sau (review, favorite, visit) khi cần biết người dùng là ai.
 */
public interface IdentityApi {

    /**
     * Hồ sơ của một người dùng chưa bị xoá mềm.
     *
     * @throws com.foodmap.common.NotFoundException mã {@code USER_NOT_FOUND} khi không có.
     */
    UserProfile getUser(UUID userId);

    /** Người dùng tồn tại và chưa bị xoá mềm. */
    boolean existsById(UUID userId);

    /**
     * Tìm người dùng cho trang quản trị: lọc theo email/tên hiển thị và vai trò.
     * Cả hai điều kiện đều tuỳ chọn — bỏ trống nghĩa là không lọc theo điều đó.
     */
    PageResponse<UserProfile> searchUsers(String query, UserRole role, Pageable pageable);

    /**
     * Đổi vai trò của một người dùng.
     *
     * @throws com.foodmap.common.NotFoundException mã {@code USER_NOT_FOUND} khi không có.
     */
    UserProfile changeRole(UUID userId, UserRole newRole);
}
