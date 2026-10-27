package com.foodmap.user.web;

import com.foodmap.auth.AuthPrincipal;
import com.foodmap.user.UserService;
import com.foodmap.user.dto.UserProfileDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "user", description = "Hồ sơ người dùng")
public class MeController {

    private final UserService userService;

    public MeController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/api/v1/me")
    @Operation(operationId = "getCurrentUser", summary = "Lấy hồ sơ của chính mình")
    public UserProfileDto getCurrentUser(@AuthenticationPrincipal AuthPrincipal principal) {
        return userService.getProfile(principal.userId());
    }
}
