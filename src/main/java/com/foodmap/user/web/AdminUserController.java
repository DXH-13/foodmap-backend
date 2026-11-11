package com.foodmap.user.web;

import com.foodmap.common.PageResponse;
import com.foodmap.identity.api.AuthPrincipal;
import com.foodmap.identity.api.UserProfile;
import com.foodmap.identity.api.UserRole;
import com.foodmap.user.application.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Quản trị người dùng, khớp operation {@code listUsers} và {@code updateUserRole}
 * trong hợp đồng.
 *
 * <p>Cả hai endpoint đều chỉ dành cho vai trò {@code ADMIN} — xem mô tả trong hợp
 * đồng. {@link org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity}
 * đã bật ở {@code SecurityConfig}; lớp URL {@code /admin/v1/**} đã chặn MODERATOR
 * và ADMIN ở tầng Security, còn đây siết tiếp xuống chỉ ADMIN.
 */
@RestController
@RequestMapping("/admin/v1/users")
@Tag(name = "admin", description = "Quản trị: kiểm duyệt, quản lý user, thống kê")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @Operation(operationId = "listUsers", summary = "Lấy danh sách người dùng")
    public PageResponse<UserProfile> listUsers(
            @Parameter(description = "Tìm theo email hoặc tên hiển thị")
            @RequestParam(required = false) String query,

            @Parameter(description = "Lọc theo vai trò")
            @RequestParam(required = false) String role,

            @RequestParam(defaultValue = "0") @Min(0) int page,

            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {

        UserRole roleFilter = parseRole(role);
        return userService.listUsers(query, roleFilter, page, size);
    }

    @PutMapping("/{userId}/role")
    @Operation(operationId = "updateUserRole", summary = "Gán vai trò cho người dùng")
    public UserProfile updateUserRole(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateRoleRequest request) {
        return userService.changeRole(principal.userId(), userId, request.role());
    }

    /**
     * Nhận vai trò dạng chuỗi rồi tự kiểm tra, thay vì để Spring tự chuyển String
     * thành enum: giá trị sai sẽ trả 400 VALIDATION_FAILED thay vì rơi vào lỗi
     * chung chung. Không phân biệt hoa thường.
     */
    private UserRole parseRole(String role) {
        if (role == null || role.isBlank()) {
            return null;
        }
        try {
            return UserRole.valueOf(role.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new com.foodmap.common.BadRequestException(
                    "VALIDATION_FAILED", "common.error.validation");
        }
    }

    /** Body của PUT /admin/v1/users/{userId}/role. */
    public record UpdateRoleRequest(@NotNull UserRole role) {
    }
}
