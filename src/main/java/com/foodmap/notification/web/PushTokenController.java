package com.foodmap.notification.web;

import com.foodmap.identity.api.AuthPrincipal;
import com.foodmap.notification.application.NotificationService;
import com.foodmap.notification.web.dto.RegisterPushTokenRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Đăng ký push token của thiết bị. Khớp operation {@code registerPushToken}
 * trong hợp đồng — trả 204, đăng ký lại cùng token thì chỉ cập nhật mốc dùng.
 */
@RestController
@RequestMapping("/app/v1/me/push-tokens")
@Tag(name = "notification", description = "Thông báo trong app")
public class PushTokenController {

    private final NotificationService notificationService;

    public PushTokenController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping
    @Operation(operationId = "registerPushToken", summary = "Đăng ký push token của thiết bị")
    public ResponseEntity<Void> registerPushToken(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody RegisterPushTokenRequest request) {
        notificationService.registerPushToken(principal.userId(), request.token(), request.platform());
        return ResponseEntity.noContent().build();
    }
}
