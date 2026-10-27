package com.foodmap.auth;

import com.foodmap.auth.dto.AuthTokensDto;
import com.foodmap.auth.dto.LoginRequest;
import com.foodmap.auth.dto.RegisterRequest;
import com.foodmap.common.exception.BadRequestException;
import com.foodmap.common.exception.UnauthorizedException;
import com.foodmap.config.props.FoodmapProperties;
import com.foodmap.user.User;
import com.foodmap.user.UserService;
import com.foodmap.user.dto.UserProfileDto;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Đăng nhập và làm mới token (FR-AUTH-03, FR-AUTH-04).
 *
 * <p>Refresh token <b>dùng một lần</b>: mỗi lần làm mới, token cũ bị thu hồi và cặp mới
 * được phát hành. Một token đã thu hồi mà vẫn được gửi lên nghĩa là nó đã bị lộ (kẻ gian
 * hoặc chủ thật đã dùng trước) — khi đó thu hồi <b>toàn bộ</b> phiên của người dùng.
 */
@Service
public class AuthService {

    private static final int REFRESH_TOKEN_BYTES = 32;
    private static final int MIN_PASSWORD_LENGTH = 8;

    private final UserService userService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final Duration refreshTokenTtl;
    private final SecureRandom random = new SecureRandom();

    /**
     * Băm sẵn một mật khẩu bất kỳ để so khi email không tồn tại. Không so thì request với
     * email lạ trả về nhanh hơn hẳn (không tốn ~250ms BCrypt) — đủ để dò ra email nào đã đăng ký.
     */
    private final String dummyPasswordHash;

    public AuthService(UserService userService,
                       RefreshTokenRepository refreshTokenRepository,
                       JwtService jwtService,
                       PasswordEncoder passwordEncoder,
                       FoodmapProperties properties) {
        this.userService = userService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenTtl = Duration.ofDays(properties.jwt().refreshTokenTtlDays());
        this.dummyPasswordHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    /**
     * Đăng ký bằng email (FR-AUTH-01) và cấp phiên luôn — người dùng không phải đăng nhập lại.
     *
     * <p>Tài khoản mới ở trạng thái <b>chưa xác minh email</b> ({@code emailVerified = false}).
     * ponytail: chưa sinh token xác minh và chưa gửi mail — thêm khi làm verify-email/resend-verification.
     */
    @Transactional
    public AuthTokensDto register(RegisterRequest request) {
        String password = request.password();
        if (password.length() < MIN_PASSWORD_LENGTH
                || password.codePoints().noneMatch(Character::isLetter)
                || password.codePoints().noneMatch(Character::isDigit)) {
            throw new BadRequestException("WEAK_PASSWORD", "auth.error.weak_password");
        }
        User user = userService.register(
                request.email().strip(),
                passwordEncoder.encode(password),
                request.displayName().strip(),
                request.preferredLocale() == null ? "vi" : request.preferredLocale());
        return issueTokens(user);
    }

    /**
     * Sai email và sai mật khẩu trả về <b>cùng một lỗi</b> — không tiết lộ email nào đã đăng ký.
     */
    @Transactional
    public AuthTokensDto login(LoginRequest request) {
        User user = userService.findActiveByEmail(request.email()).orElse(null);

        String hash = user == null ? dummyPasswordHash : user.getPasswordHash();
        if (!passwordEncoder.matches(request.password(), hash) || user == null) {
            throw new UnauthorizedException("INVALID_CREDENTIALS", "auth.error.invalid_credentials");
        }
        return issueTokens(user);
    }

    /**
     * {@code noRollbackFor}: khi phát hiện token bị dùng lại, lệnh thu hồi toàn bộ phiên
     * phải được ghi xuống CSDL dù request vẫn kết thúc bằng lỗi 401.
     */
    @Transactional(noRollbackFor = UnauthorizedException.class)
    public AuthTokensDto refresh(String rawRefreshToken) {
        Instant now = Instant.now();
        RefreshToken token = refreshTokenRepository.findByTokenHash(hash(rawRefreshToken))
                .orElseThrow(AuthService::invalidToken);

        if (token.getRevokedAt() != null || refreshTokenRepository.revokeIfActive(token.getId(), now) == 0) {
            refreshTokenRepository.revokeAllActive(token.getUserId(), now);
            throw invalidToken();
        }
        if (token.getExpiresAt().isBefore(now)) {
            throw new UnauthorizedException("TOKEN_EXPIRED", "auth.error.token_expired");
        }

        User user = userService.findActiveById(token.getUserId()).orElseThrow(AuthService::invalidToken);
        return issueTokens(user);
    }

    private AuthTokensDto issueTokens(User user) {
        byte[] bytes = new byte[REFRESH_TOKEN_BYTES];
        random.nextBytes(bytes);
        String rawRefreshToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        refreshTokenRepository.save(new RefreshToken(
                user.getId(), hash(rawRefreshToken), Instant.now().plus(refreshTokenTtl)));

        String accessToken = jwtService.issueAccessToken(
                user.getId(), user.getRole().name(), user.isEmailVerified());

        return new AuthTokensDto(
                accessToken, rawRefreshToken, jwtService.accessTokenTtlSeconds(), UserProfileDto.from(user));
    }

    /** SHA-256 dạng hex, đúng 64 ký tự — khớp cột {@code token_hash VARCHAR(64)}. */
    private static String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            // Mọi JVM đều bắt buộc có SHA-256
            throw new IllegalStateException(ex);
        }
    }

    private static UnauthorizedException invalidToken() {
        return new UnauthorizedException("INVALID_TOKEN", "auth.error.invalid_token");
    }
}
