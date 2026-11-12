package com.foodmap.identity;

import com.foodmap.common.BadRequestException;
import com.foodmap.common.ConflictException;
import com.foodmap.common.UnauthorizedException;
import com.foodmap.identity.application.AuthService;
import com.foodmap.identity.application.EmailService;
import com.foodmap.identity.application.JwtService;
import com.foodmap.identity.domain.EmailVerificationToken;
import com.foodmap.identity.domain.PasswordResetToken;
import com.foodmap.identity.domain.RefreshToken;
import com.foodmap.identity.domain.TokenGenerator;
import com.foodmap.identity.domain.User;
import com.foodmap.identity.infrastructure.EmailVerificationTokenRepository;
import com.foodmap.identity.infrastructure.PasswordResetTokenRepository;
import com.foodmap.identity.infrastructure.RefreshTokenRepository;
import com.foodmap.identity.infrastructure.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test use case xác thực ở tầng service, không cần CSDL hay Docker.
 *
 * <p>Repository và {@link EmailService} là mock; {@link JwtService} và
 * {@link PasswordEncoder} là thật để kiểm tra token/mật khẩu đúng nghĩa.
 * Integration test với Testcontainers vẫn cần CI chạy (xem
 * {@code FoodmapBackendApplicationTests}).
 */
class AuthServiceTest {

    private static final String SECRET = "test-secret-that-is-long-enough-for-hs256-0123456789";

    private UserRepository users;
    private RefreshTokenRepository refreshTokens;
    private PasswordResetTokenRepository resetTokens;
    private EmailVerificationTokenRepository verificationTokens;
    private EmailService emailService;
    private PasswordEncoder passwordEncoder;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        refreshTokens = mock(RefreshTokenRepository.class);
        resetTokens = mock(PasswordResetTokenRepository.class);
        verificationTokens = mock(EmailVerificationTokenRepository.class);
        emailService = mock(EmailService.class);
        passwordEncoder = new BCryptPasswordEncoder();
        JwtService jwtService = new JwtService(
                new com.foodmap.identity.JwtProperties(SECRET, 15, 30, "foodmap"));
        authService = new AuthService(users, refreshTokens, resetTokens, verificationTokens,
                passwordEncoder, jwtService, emailService,
                new com.foodmap.identity.JwtProperties(SECRET, 15, 30, "foodmap"),
                mock(org.springframework.context.ApplicationEventPublisher.class));
        // Giả lập JPA persist: save() gán id cho entity (ngoài đời CSDL làm việc này).
        when(users.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });
    }

    private User existingUser() {
        User user = User.register("an@example.com",
                passwordEncoder.encode("Password1"), "An", "vi");
        user.setId(UUID.randomUUID());
        return user;
    }

    @Test
    @DisplayName("Đăng ký: tạo user, gửi email xác minh, trả cặp token + hồ sơ")
    void registerCreatesUserAndIssuesTokens() {
        when(users.existsByEmailIgnoreCaseAndDeletedAtIsNull("an@example.com"))
                .thenReturn(false);

        AuthService.AuthResult result = authService.register(
                "An@Example.com", "Password1", "An", "vi");

        assertThat(result.accessToken()).isNotBlank();
        assertThat(result.refreshToken()).isNotBlank();
        assertThat(result.expiresIn()).isEqualTo(900L);
        assertThat(result.user().email()).isEqualTo("an@example.com");
        assertThat(result.user().emailVerified()).isFalse();
        verify(users).save(any(User.class));
        verify(emailService).sendVerificationEmail(any(User.class), anyString());
        verify(refreshTokens).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Đăng ký: email đã tồn tại → 409 EMAIL_ALREADY_EXISTS")
    void registerDuplicateEmail() {
        when(users.existsByEmailIgnoreCaseAndDeletedAtIsNull("an@example.com"))
                .thenReturn(true);

        assertThatThrownBy(() -> authService.register("an@example.com", "Password1", "An", "vi"))
                .isInstanceOf(ConflictException.class)
                .satisfies(ex -> assertThat(((ConflictException) ex).getCode())
                        .isEqualTo("EMAIL_ALREADY_EXISTS"));
        verify(emailService, never()).sendVerificationEmail(any(), anyString());
    }

    @Test
    @DisplayName("Đăng ký: mật khẩu yếu (không có chữ số) → 400 WEAK_PASSWORD")
    void registerWeakPassword() {
        assertThatThrownBy(() -> authService.register("an@example.com", "password", "An", "vi"))
                .isInstanceOf(BadRequestException.class)
                .satisfies(ex -> assertThat(((BadRequestException) ex).getCode())
                        .isEqualTo("WEAK_PASSWORD"));
    }

    @Test
    @DisplayName("Đăng nhập: đúng mật khẩu → trả token; sai mật khẩu → 401")
    void loginSuccessAndWrongPassword() {
        User user = existingUser();
        when(users.findByEmailIgnoreCaseAndDeletedAtIsNull("an@example.com"))
                .thenReturn(Optional.of(user));

        AuthService.AuthResult result = authService.login("an@example.com", "Password1");
        assertThat(result.user().id()).isEqualTo(user.getId());

        assertThatThrownBy(() -> authService.login("an@example.com", "Wrongpass1"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("Đăng nhập: email không tồn tại → 401, không lộ email có đăng ký hay không")
    void loginUnknownEmail() {
        when(users.findByEmailIgnoreCaseAndDeletedAtIsNull(anyString()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login("khong@co.com", "Password1"))
                .isInstanceOf(UnauthorizedException.class)
                .satisfies(ex -> assertThat(((UnauthorizedException) ex).getCode())
                        .isEqualTo("INVALID_CREDENTIALS"));
    }

    @Test
    @DisplayName("Làm mới token: xoay vòng — token cũ bị thu hồi, trả cặp mới")
    void refreshRotatesToken() {
        User user = existingUser();
        RefreshToken oldToken = new RefreshToken(user, "hash-cu",
                Instant.now().plusSeconds(3600));
        when(refreshTokens.findByTokenHash(anyString())).thenReturn(Optional.of(oldToken));
        when(users.findByIdAndDeletedAtIsNull(user.getId())).thenReturn(Optional.of(user));

        AuthService.AuthResult result = authService.refresh("raw-token-cu");

        assertThat(oldToken.isRevoked()).isTrue();
        assertThat(result.refreshToken()).isNotBlank();
        assertThat(result.refreshToken()).isNotEqualTo("raw-token-cu");
        verify(refreshTokens).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Làm mới token: dùng lại token đã thu hồi → 401 và vô hiệu toàn bộ phiên")
    void refreshReuseRevokesAllSessions() {
        User user = existingUser();
        RefreshToken revoked = new RefreshToken(user, "hash-cu",
                Instant.now().plusSeconds(3600));
        revoked.revoke();
        when(refreshTokens.findByTokenHash(anyString())).thenReturn(Optional.of(revoked));

        assertThatThrownBy(() -> authService.refresh("raw-token-cu"))
                .isInstanceOf(UnauthorizedException.class);
        verify(refreshTokens).revokeAllByUserId(eq(user.getId()), any(Instant.class));
    }

    @Test
    @DisplayName("Đặt lại mật khẩu: token sai → 400 RESET_TOKEN_INVALID")
    void resetPasswordInvalidToken() {
        when(resetTokens.findByTokenHashAndUsedAtIsNull(anyString()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.resetPassword("token-sai", "Newpass1"))
                .isInstanceOf(BadRequestException.class)
                .satisfies(ex -> assertThat(((BadRequestException) ex).getCode())
                        .isEqualTo("RESET_TOKEN_INVALID"));
    }

    @Test
    @DisplayName("Đặt lại mật khẩu: token đúng → đổi mật khẩu, đánh dấu đã dùng, vô hiệu phiên cũ")
    void resetPasswordSuccess() {
        User user = existingUser();
        PasswordResetToken token = new PasswordResetToken(user, "hash",
                Instant.now().plusSeconds(3600));
        when(resetTokens.findByTokenHashAndUsedAtIsNull(anyString()))
                .thenReturn(Optional.of(token));

        authService.resetPassword("raw-token", "Newpass1");

        assertThat(token.isUsed()).isTrue();
        assertThat(passwordEncoder.matches("Newpass1", user.getPasswordHash())).isTrue();
        verify(refreshTokens).revokeAllByUserId(eq(user.getId()), any(Instant.class));
    }

    @Test
    @DisplayName("Xác minh email: token đúng → đánh dấu đã xác minh")
    void verifyEmailSuccess() {
        User user = existingUser();
        EmailVerificationToken token = new EmailVerificationToken(user, "hash",
                Instant.now().plusSeconds(3600));
        when(verificationTokens.findByTokenHashAndUsedAtIsNull(anyString()))
                .thenReturn(Optional.of(token));

        authService.verifyEmail("raw-token");

        assertThat(token.isUsed()).isTrue();
        assertThat(user.isEmailVerified()).isTrue();
        assertThat(user.getEmailVerifiedAt()).isNotNull();
    }

    @Test
    @DisplayName("Quên mật khẩu: email không tồn tại → vẫn thành công, không gửi mail (không lộ)")
    void forgotPasswordUnknownEmailDoesNotLeak() {
        when(users.findByEmailIgnoreCaseAndDeletedAtIsNull(anyString()))
                .thenReturn(Optional.empty());

        authService.forgotPassword("khong@co.com");

        verify(emailService, never()).sendPasswordResetEmail(any(), anyString());
        verify(resetTokens, never()).save(any());
    }

    @Test
    @DisplayName("Đăng xuất: thu hồi refresh token; token lạ thì vẫn thành công (luỹ đẳng)")
    void logoutRevokesOrNoops() {
        User user = existingUser();
        RefreshToken token = new RefreshToken(user, "hash",
                Instant.now().plusSeconds(3600));
        when(refreshTokens.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        authService.logout("raw-token");
        assertThat(token.isRevoked()).isTrue();

        when(refreshTokens.findByTokenHash(anyString())).thenReturn(Optional.empty());
        authService.logout("token-la");
    }

    @Test
    @DisplayName("Token hash: SHA-256 hex 64 ký tự, token gốc không lưu được")
    void tokenHashing() {
        String raw = TokenGenerator.generateRawToken();
        String hash = TokenGenerator.sha256Hex(raw);
        assertThat(hash).hasSize(64);
        assertThat(hash).isNotEqualTo(raw);
        assertThat(TokenGenerator.sha256Hex(raw)).isEqualTo(hash);
    }

    private static UUID eq(UUID id) {
        return org.mockito.ArgumentMatchers.eq(id);
    }
}
