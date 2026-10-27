package com.foodmap.auth.web;

import com.foodmap.auth.AuthService;
import com.foodmap.auth.dto.AuthTokensDto;
import com.foodmap.auth.dto.LoginRequest;
import com.foodmap.auth.dto.RefreshTokenRequest;
import com.foodmap.auth.dto.RegisterRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Đặt ở core chứ không ở app module: cả app di động (qua app-public) lẫn trang quản trị
 * (qua app-admin) đều đăng nhập bằng endpoint này. Luật đường dẫn của từng app quyết định
 * endpoint nào được mở — ví dụ app-admin không mở {@code register}.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "auth", description = "Đăng ký, đăng nhập, quản lý phiên")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            operationId = "register",
            summary = "Đăng ký tài khoản mới",
            description = "Email đã đăng ký trả 409 EMAIL_ALREADY_EXISTS; mật khẩu yếu trả 400 WEAK_PASSWORD.")
    public AuthTokensDto register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    @Operation(operationId = "login", summary = "Đăng nhập")
    public AuthTokensDto login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    @Operation(
            operationId = "refreshToken",
            summary = "Làm mới access token",
            description = """
                    Refresh token dùng một lần. Mỗi lần gọi sinh cặp token mới và vô hiệu token cũ.
                    Dùng lại token đã tiêu sẽ vô hiệu toàn bộ phiên của người dùng đó.""")
    public AuthTokensDto refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refresh(request.refreshToken());
    }
}
