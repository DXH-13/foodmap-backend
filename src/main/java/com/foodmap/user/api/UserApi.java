package com.foodmap.user.api;

import com.foodmap.common.PageResponse;
import com.foodmap.identity.api.UserProfile;
import com.foodmap.identity.api.UserRole;

import java.util.UUID;

/**
 * Giao diện công khai của module {@code user} cho module khác gọi qua {@code api/}.
 *
 * <p>Module {@code user} sở hữu hồ sơ và quản trị người dùng; dữ liệu tài khoản gốc
 * vẫn nằm ở module {@code identity} và chỉ được chạm qua {@code IdentityApi}.
 */
public interface UserApi {

    /** Hồ sơ của chính mình (hoặc của một người dùng bất kỳ, theo id). */
    UserProfile getProfile(UUID userId);

    /** Danh sách người dùng cho trang quản trị. */
    PageResponse<UserProfile> listUsers(String query, UserRole role, int page, int size);

    /**
     * Đổi vai trò của một người dùng. Admin không được tự đổi vai trò của chính mình
     * (tránh tự khoá mình khỏi trang quản trị).
     */
    UserProfile changeRole(UUID actorId, UUID userId, UserRole newRole);
}
