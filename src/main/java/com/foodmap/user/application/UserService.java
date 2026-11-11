package com.foodmap.user.application;

import com.foodmap.common.ForbiddenException;
import com.foodmap.common.PageResponse;
import com.foodmap.identity.api.IdentityApi;
import com.foodmap.identity.api.UserProfile;
import com.foodmap.identity.api.UserRole;
import com.foodmap.user.api.UserApi;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Hồ sơ và quản trị người dùng (Phase 1).
 *
 * <p>Module này không sở hữu bảng {@code users} — mọi đọc/ghi tài khoản đều đi qua
 * {@link IdentityApi}. Nhờ vậy sau này tách {@code identity} ra, {@code user} không
 * phải sửa.
 */
@Service
@Transactional(readOnly = true)
public class UserService implements UserApi {

    private final IdentityApi identity;

    public UserService(IdentityApi identity) {
        this.identity = identity;
    }

    @Override
    public UserProfile getProfile(UUID userId) {
        return identity.getUser(userId);
    }

    @Override
    public PageResponse<UserProfile> listUsers(String query, UserRole role, int page, int size) {
        return identity.searchUsers(query, role, PageRequest.of(page, size));
    }

    @Override
    @Transactional
    public UserProfile changeRole(UUID actorId, UUID userId, UserRole newRole) {
        if (actorId.equals(userId)) {
            throw new ForbiddenException("FORBIDDEN", "common.error.forbidden");
        }
        return identity.changeRole(userId, newRole);
    }
}
