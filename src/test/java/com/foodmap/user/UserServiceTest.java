package com.foodmap.user;

import com.foodmap.common.ForbiddenException;
import com.foodmap.common.PageResponse;
import com.foodmap.identity.api.IdentityApi;
import com.foodmap.identity.api.UserProfile;
import com.foodmap.identity.api.UserRole;
import com.foodmap.user.application.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test {@link UserService} — module {@code user} chỉ uỷ quyền sang
 * {@code IdentityApi}, không chạm trực tiếp vào bảng users.
 */
class UserServiceTest {

    private final IdentityApi identity = mock(IdentityApi.class);
    private final UserService userService = new UserService(identity);

    private static UserProfile profile(UUID id, String email) {
        return new UserProfile(id, email, "An", null, UserRole.USER, "vi", false,
                Instant.now());
    }

    @Test
    @DisplayName("Lấy hồ sơ: uỷ quyền sang IdentityApi")
    void getProfileDelegates() {
        UUID id = UUID.randomUUID();
        when(identity.getUser(id)).thenReturn(profile(id, "an@example.com"));

        assertThat(userService.getProfile(id).email()).isEqualTo("an@example.com");
        verify(identity).getUser(id);
    }

    @Test
    @DisplayName("Danh sách user: uỷ quyền kèm phân trang")
    void listUsersDelegates() {
        PageResponse<UserProfile> page = new PageResponse<>(
                List.of(profile(UUID.randomUUID(), "an@example.com")), 0, 20, 1, 1);
        when(identity.searchUsers(eq("an"), eq(null), any()))
                .thenReturn(page);

        PageResponse<UserProfile> result = userService.listUsers("an", null, 0, 20);

        assertThat(result.content()).hasSize(1);
        verify(identity).searchUsers(eq("an"), eq(null), eq(PageRequest.of(0, 20)));
    }

    @Test
    @DisplayName("Đổi vai trò: admin không được tự đổi vai trò của chính mình")
    void changeRoleSelfForbidden() {
        UUID adminId = UUID.randomUUID();

        assertThatThrownBy(() -> userService.changeRole(adminId, adminId, UserRole.MODERATOR))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("Đổi vai trò: đổi cho người khác thì uỷ quyền sang IdentityApi")
    void changeRoleOtherDelegates() {
        UUID adminId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        when(identity.changeRole(targetId, UserRole.MODERATOR))
                .thenReturn(profile(targetId, "an@example.com"));

        UserProfile result = userService.changeRole(adminId, targetId, UserRole.MODERATOR);

        assertThat(result.id()).isEqualTo(targetId);
        verify(identity).changeRole(targetId, UserRole.MODERATOR);
    }
}
