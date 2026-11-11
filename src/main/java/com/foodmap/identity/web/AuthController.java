package com.foodmap.identity.web;

import com.foodmap.identity.api.AuthPrincipal;
import com.foodmap.identity.application.AuthService;
import com.foodmap.identity.web.dto.AuthTokensResponse;
import com.foodmap.identity.web.dto.ForgotPasswordRequest;
import com.foodmap.identity.web.dto.LoginRequest;
import com.foodmap.identity.web.dto.LogoutRequest;
import com.foodmap.identity.web.dto.RefreshTokenRequest;
import com.foodmap.identity.web.dto.RegisterRequest;
import com.foodmap.identity.web.dto.ResetPasswordRequest;
import com.foodmap.identity.web.dto.VerifyEmailRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Endpoint xác thực (Phase 1), khớp các operation {@code auth} trong
 * docs/SDD/api/openapi.yaml. Controller chỉ kiểm tra đầu vào và gọi service —
 * không chứa quy tắc nghiệp vụ.
 */
@RestController
@RequestMapping("/app/v1/auth")
@Tag(name = "auth", description = "Đăng ký, đăng nhập, token, mật khẩu")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(operationId = "register", summary = "Đăng ký tài khoản mới")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthTokensResponse register(@Valid @RequestBody RegisterRequest request) {
        return AuthTokensResponse.from(authService.register(
                request.email(), request.password(), request.displayName(), request.preferredLocale()));
    }

    @PostMapping("/login")
    @Operation(operationId = "login", summary = "Đăng nhập")
    public AuthTokensResponse login(@Valid @RequestBody LoginRequest request) {
        return AuthTokensResponse.from(authService.login(request.email(), request.password()));
    }

    @PostMapping("/refresh")
    @Operation(operationId = "refreshToken", summary = "Làm mới access token")
    public AuthTokensResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return AuthTokensResponse.from(authService.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    @Operation(operationId = "logout", summary = "Đăng xuất")
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/forgot-password")
    @Operation(operationId = "forgotPassword", summary = "Yêu cầu đặt lại mật khẩu")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.email());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/reset-password")
    @Operation(operationId = "resetPassword", summary = "Đặt lại mật khẩu bằng token")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/verify-email")
    @Operation(operationId = "verifyEmail", summary = "Xác minh email bằng token")
    public ResponseEntity<Void> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        authService.verifyEmail(request.token());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/resend-verification")
    @Operation(operationId = "resendVerificationEmail", summary = "Gửi lại email xác minh")
    public ResponseEntity<Void> resendVerification(@AuthenticationPrincipal AuthPrincipal principal) {
        authService.resendVerification(principal.userId());
        return ResponseEntity.accepted().build();
    }
}
