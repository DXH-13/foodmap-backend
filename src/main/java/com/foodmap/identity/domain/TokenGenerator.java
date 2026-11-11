package com.foodmap.identity.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Sinh và băm token có trạng thái (refresh token, token xác minh email, token đặt
 * lại mật khẩu).
 *
 * <p>Token gốc chỉ tồn tại trong email gửi cho người dùng và trong request họ gửi lại
 * — <b>không bao giờ lưu token gốc vào CSDL</b>, chỉ lưu SHA-256 hex (64 ký tự, khớp
 * cột {@code token_hash} trong V1). Ai đọc được CSDL cũng không đăng nhập được bằng
 * hash.
 */
public final class TokenGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();

    /** 32 byte ngẫu nhiên — đủ dài để không đoán được (256 bit entropy). */
    private static final int TOKEN_BYTES = 32;

    private TokenGenerator() {
    }

    /** Token gốc, dạng Base64 URL-safe không padding — an toàn để đặt trong URL/email. */
    public static String generateRawToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** SHA-256 hex (64 ký tự) của token gốc — giá trị duy nhất được lưu xuống CSDL. */
    public static String sha256Hex(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            // SHA-256 luôn có mặt trên mọi JVM — không tới được đây.
            throw new IllegalStateException("SHA-256 khong co san", ex);
        }
    }
}
