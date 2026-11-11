package com.foodmap.user.web;

import com.foodmap.identity.api.AuthPrincipal;
import com.foodmap.identity.api.UserProfile;
import com.foodmap.user.application.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Hồ sơ của chính mình, khớp operation {@code getCurrentUser} trong hợp đồng.
 * Cập nhật hồ sơ (PATCH) và yêu cầu xoá tài khoản (DELETE) thuộc các phase sau —
 * chưa làm ở bước này.
 */
@RestController
@RequestMapping("/app/v1/me")
@Tag(name = "user", description = "Hồ sơ người dùng hiện tại")
public class MeController {

    private final UserService userService;

    public MeController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @Operation(operationId = "getCurrentUser", summary = "Lấy hồ sơ của chính mình")
    public UserProfile getCurrentUser(@AuthenticationPrincipal AuthPrincipal principal) {
        return userService.getProfile(principal.userId());
    }
}
