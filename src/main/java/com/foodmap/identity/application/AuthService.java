package com.foodmap.identity.application;

import com.foodmap.common.BadRequestException;
import com.foodmap.common.ConflictException;
import com.foodmap.common.NotFoundException;
import com.foodmap.common.PageResponse;
import com.foodmap.common.UnauthorizedException;
import com.foodmap.identity.JwtProperties;
import com.foodmap.identity.api.IdentityApi;
import com.foodmap.identity.api.UserProfile;
import com.foodmap.identity.domain.EmailVerificationToken;
import com.foodmap.identity.domain.PasswordResetToken;
import com.foodmap.identity.domain.RefreshToken;
import com.foodmap.identity.domain.TokenGenerator;
import com.foodmap.identity.domain.User;
import com.foodmap.identity.api.UserRegistered;
import com.foodmap.identity.api.UserRole;
import com.foodmap.identity.infrastructure.EmailVerificationTokenRepository;
import com.foodmap.identity.infrastructure.PasswordResetTokenRepository;
import com.foodmap.identity.infrastructure.RefreshTokenRepository;
import com.foodmap.identity.infrastructure.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Use case xác thực (Phase 1), khớp các operation {@code auth} trong
 * docs/SDD/api/openapi.yaml.
 *
 * <p>Quy tắc chung:
 * <ul>
 *   <li>Mật khẩu tối thiểu 8 ký tự, có ít nhất một chữ cái và một chữ số — xem
 *       mô tả của {@code RegisterRequest} trong hợp đồng.</li>
 *   <li>Token có trạng thái (refresh, xác minh, đặt lại mật khẩu) chỉ lưu SHA-256
 *       hash; refresh token dùng một lần và xoay vòng mỗi lần làm mới.</li>
 *   <li>Đăng nhập không yêu cầu email đã xác minh — trạng thái xác minh nằm trong
 *       claim {@code emailVerified} của access token để các tính năng sau tự kiểm tra
 *       (FR-AUTH-02). Tài khoản đã xoá mềm coi như không tồn tại.</li>
 * </ul>
 */
@Service
@Transactional
public class AuthService implements IdentityApi {

    /** Token xác minh email sống 24 giờ — hợp đồng không quy định, chọn theo thông lệ. */
    private static final Duration VERIFICATION_TOKEN_TTL = Duration.ofHours(24);

    /** Token đặt lại mật khẩu sống 1 giờ — hợp đồng không quy định, chọn theo thông lệ. */
    private static final Duration RESET_TOKEN_TTL = Duration.ofHours(1);

    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).{8,}$");

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordResetTokenRepository resetTokens;
    private final EmailVerificationTokenRepository verificationTokens;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final ApplicationEventPublisher events;
    private final Duration refreshTokenTtl;

    public AuthService(UserRepository users,
                       RefreshTokenRepository refreshTokens,
                       PasswordResetTokenRepository resetTokens,
                       EmailVerificationTokenRepository verificationTokens,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       EmailService emailService,
                       JwtProperties jwtProperties,
                       ApplicationEventPublisher events) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.resetTokens = resetTokens;
        this.verificationTokens = verificationTokens;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.emailService = emailService;
        this.events = events;
        this.refreshTokenTtl = Duration.ofDays(jwtProperties.refreshTokenTtlDays());
    }

    /** Kết quả đăng ký / đăng nhập / làm mới token — khớp schema {@code AuthTokens}. */
    public record AuthResult(String accessToken, String refreshToken, long expiresIn, UserProfile user) {
    }

    /**
     * Đăng ký tài khoản mới (201). Tạo user, gửi email xác minh và trả luôn cặp token
     * để client không phải đăng nhập lại.
     */
    public AuthResult register(String email, String password, String displayName, String preferredLocale) {
        String normalizedEmail = normalizeEmail(email);
        checkPasswordStrength(password);
        String locale = preferredLocale == null ? "vi" : preferredLocale;
        if (!locale.equals("vi") && !locale.equals("en")) {
            throw new BadRequestException("VALIDATION_FAILED", "common.error.validation");
        }
        if (users.existsByEmailIgnoreCaseAndDeletedAtIsNull(normalizedEmail)) {
            throw new ConflictException("EMAIL_ALREADY_EXISTS", "auth.error.email_already_exists");
        }

        User user = User.register(normalizedEmail, passwordEncoder.encode(password),
                displayName.trim(), locale);
        users.save(user);

        // Phát trong cùng transaction với việc tạo tài khoản — outbox ghi lại,
        // module notification nghe sau commit để gửi chào mừng (Bước 5).
        events.publishEvent(new UserRegistered(user.getId(), user.getEmail()));
        issueVerificationEmail(user);
        return issueTokens(user);
    }

    /** Đăng nhập (200). Sai email hoặc sai mật khẩu đều trả cùng một lỗi 401. */
    public AuthResult login(String email, String password) {
        User user = users.findByEmailIgnoreCaseAndDeletedAtIsNull(normalizeEmail(email))
                .orElseThrow(() -> new UnauthorizedException(
                        "INVALID_CREDENTIALS", "auth.error.invalid_credentials"));
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new UnauthorizedException("INVALID_CREDENTIALS", "auth.error.invalid_credentials");
        }
        return issueTokens(user);
    }

    /**
     * Làm mới access token (200). Refresh token dùng một lần: token cũ bị thu hồi,
     * cấp cặp mới. Dùng lại token đã thu hồi là dấu hiệu token bị đánh cắp — vô hiệu
     * toàn bộ phiên của người dùng đó rồi mới trả 401.
     */
    public AuthResult refresh(String rawRefreshToken) {
        RefreshToken token = refreshTokens.findByTokenHash(TokenGenerator.sha256Hex(rawRefreshToken))
                .orElseThrow(() -> new UnauthorizedException(
                        "INVALID_TOKEN", "auth.error.invalid_token"));
        if (token.isRevoked()) {
            refreshTokens.revokeAllByUserId(token.getUser().getId(), Instant.now());
            throw new UnauthorizedException("INVALID_TOKEN", "auth.error.invalid_token");
        }
        if (token.isExpired()) {
            throw new UnauthorizedException("TOKEN_EXPIRED", "auth.error.token_expired");
        }
        User user = loadUser(token.getUser().getId());
        token.revoke();
        return issueTokens(user);
    }

    /**
     * Đăng xuất (204). Thu hồi refresh token nếu còn — không tìm thấy thì coi như
     * đã đăng xuất rồi, vẫn trả thành công (luỹ đẳng).
     */
    public void logout(String rawRefreshToken) {
        refreshTokens.findByTokenHash(TokenGenerator.sha256Hex(rawRefreshToken))
                .ifPresent(token -> {
                    token.revoke();
                    refreshTokens.save(token);
                });
    }

    /**
     * Yêu cầu đặt lại mật khẩu (202). Luôn thành công giống hệt nhau dù email có tồn
     * tại hay không — để không lộ email nào đã đăng ký (xem mô tả trong hợp đồng).
     */
    public void forgotPassword(String email) {
        users.findByEmailIgnoreCaseAndDeletedAtIsNull(normalizeEmail(email))
                .ifPresent(user -> {
                    resetTokens.invalidateUnusedByUserId(user.getId(), Instant.now());
                    String rawToken = TokenGenerator.generateRawToken();
                    resetTokens.save(new PasswordResetToken(
                            user, TokenGenerator.sha256Hex(rawToken), Instant.now().plus(RESET_TOKEN_TTL)));
                    emailService.sendPasswordResetEmail(user, rawToken);
                });
    }

    /** Đặt lại mật khẩu bằng token (204). Token dùng một lần; đổi mật khẩu xong thì toàn bộ phiên cũ bị vô hiệu. */
    public void resetPassword(String rawToken, String newPassword) {
        PasswordResetToken token = resetTokens
                .findByTokenHashAndUsedAtIsNull(TokenGenerator.sha256Hex(rawToken))
                .orElseThrow(() -> new BadRequestException(
                        "RESET_TOKEN_INVALID", "auth.error.reset_token_invalid"));
        if (token.isExpired()) {
            throw new BadRequestException("RESET_TOKEN_EXPIRED", "auth.error.reset_token_expired");
        }
        checkPasswordStrength(newPassword);
        User user = token.getUser();
        user.changePassword(passwordEncoder.encode(newPassword));
        token.markUsed();
        refreshTokens.revokeAllByUserId(user.getId(), Instant.now());
    }

    /** Xác minh email bằng token (204). Token dùng một lần. */
    public void verifyEmail(String rawToken) {
        EmailVerificationToken token = verificationTokens
                .findByTokenHashAndUsedAtIsNull(TokenGenerator.sha256Hex(rawToken))
                .orElseThrow(() -> new BadRequestException(
                        "INVALID_TOKEN", "auth.error.invalid_token"));
        if (token.isExpired()) {
            throw new BadRequestException("TOKEN_EXPIRED", "auth.error.token_expired");
        }
        token.markUsed();
        token.getUser().markEmailVerified();
    }

    /**
     * Gửi lại email xác minh (202). Đã xác minh rồi thì không gửi nữa nhưng vẫn trả
     * thành công (luỹ đẳng) — client không phân biệt được hai trường hợp.
     */
    public void resendVerification(UUID userId) {
        User user = loadUser(userId);
        if (user.isEmailVerified()) {
            return;
        }
        issueVerificationEmail(user);
    }

    // ── IdentityApi ──────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public UserProfile getUser(UUID userId) {
        return UserProfile.from(loadUser(userId));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(UUID userId) {
        return users.findByIdAndDeletedAtIsNull(userId).isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserProfile> searchUsers(String query, UserRole role, Pageable pageable) {
        String normalizedQuery = query == null || query.isBlank() ? null : query.trim();
        return PageResponse.from(users.search(normalizedQuery, role, pageable), UserProfile::from);
    }

    @Override
    public UserProfile changeRole(UUID userId, UserRole newRole) {
        User user = loadUser(userId);
        user.changeRole(newRole);
        return UserProfile.from(user);
    }

    // ── Nội bộ ───────────────────────────────────────────────────────────────

    private void issueVerificationEmail(User user) {
        verificationTokens.invalidateUnusedByUserId(user.getId(), Instant.now());
        String rawToken = TokenGenerator.generateRawToken();
        verificationTokens.save(new EmailVerificationToken(
                user, TokenGenerator.sha256Hex(rawToken), Instant.now().plus(VERIFICATION_TOKEN_TTL)));
        emailService.sendVerificationEmail(user, rawToken);
    }

    private AuthResult issueTokens(User user) {
        String accessToken = jwtService.issueAccessToken(
                user.getId(), user.getRole().name(), user.isEmailVerified());
        String rawRefreshToken = TokenGenerator.generateRawToken();
        refreshTokens.save(new RefreshToken(
                user, TokenGenerator.sha256Hex(rawRefreshToken), Instant.now().plus(refreshTokenTtl)));
        return new AuthResult(accessToken, rawRefreshToken,
                jwtService.accessTokenTtlSeconds(), UserProfile.from(user));
    }

    private User loadUser(UUID userId) {
        return users.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "user.error.not_found"));
    }

    private void checkPasswordStrength(String password) {
        if (password == null || !PASSWORD_PATTERN.matcher(password).matches()) {
            throw new BadRequestException("WEAK_PASSWORD", "auth.error.weak_password");
        }
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
