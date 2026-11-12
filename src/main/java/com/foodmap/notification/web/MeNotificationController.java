package com.foodmap.notification.web;

import com.foodmap.identity.api.AuthPrincipal;
import com.foodmap.notification.application.NotificationService;
import com.foodmap.notification.web.dto.MarkNotificationsReadRequest;
import com.foodmap.notification.web.dto.NotificationListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Thông báo của chính mình. Khớp các operation {@code listNotifications} và
 * {@code markNotificationsRead} trong hợp đồng.
 */
@RestController
@RequestMapping("/app/v1/me/notifications")
@Tag(name = "notification", description = "Thông báo trong app")
public class MeNotificationController {

    private final NotificationService notificationService;

    public MeNotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @Operation(operationId = "listNotifications", summary = "Lấy danh sách thông báo")
    public NotificationListResponse listNotifications(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return notificationService.listNotifications(principal.userId(), unreadOnly, page, size);
    }

    @PostMapping("/read")
    @Operation(operationId = "markNotificationsRead", summary = "Đánh dấu thông báo đã đọc")
    public ResponseEntity<Void> markNotificationsRead(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestBody MarkNotificationsReadRequest request) {
        notificationService.markNotificationsRead(
                principal.userId(),
                request == null ? null : request.notificationIds());
        return ResponseEntity.noContent().build();
    }
}
