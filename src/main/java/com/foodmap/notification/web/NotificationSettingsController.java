package com.foodmap.notification.web;

import com.foodmap.identity.api.AuthPrincipal;
import com.foodmap.notification.application.NotificationService;
import com.foodmap.notification.web.dto.NotificationSettingsDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Cài đặt nhận thông báo của chính mình. Khớp các operation
 * {@code getNotificationSettings} và {@code updateNotificationSettings} trong
 * hợp đồng.
 */
@RestController
@RequestMapping("/app/v1/me/notification-settings")
@Tag(name = "notification", description = "Thông báo trong app")
public class NotificationSettingsController {

    private final NotificationService notificationService;

    public NotificationSettingsController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @Operation(operationId = "getNotificationSettings", summary = "Lấy cấu hình nhận thông báo đẩy")
    public NotificationSettingsDto getNotificationSettings(
            @AuthenticationPrincipal AuthPrincipal principal) {
        return notificationService.getSettings(principal.userId());
    }

    @PutMapping
    @Operation(operationId = "updateNotificationSettings", summary = "Cập nhật cấu hình nhận thông báo đẩy")
    public NotificationSettingsDto updateNotificationSettings(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody NotificationSettingsDto request) {
        return notificationService.updateSettings(principal.userId(), request.enabledTypes());
    }
}
