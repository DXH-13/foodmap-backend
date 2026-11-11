package com.foodmap.identity.infrastructure;

import com.foodmap.identity.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Truy vấn bảng {@code refresh_tokens}. Chỉ lưu SHA-256 hash — xem
 * {@link com.foodmap.identity.domain.TokenGenerator}.
 */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    Optional<RefreshToken> findByTokenHashAndRevokedAtIsNull(String tokenHash);

    /**
     * Vô hiệu toàn bộ phiên của một người dùng — dùng khi phát hiện dùng lại refresh
     * token đã thu hồi (dấu hiệu token bị đánh cắp), và khi đổi mật khẩu.
     */
    @Modifying
    @Query("""
            UPDATE RefreshToken t SET t.revokedAt = :now
            WHERE t.user.id = :userId AND t.revokedAt IS NULL
            """)
    void revokeAllByUserId(@Param("userId") UUID userId, @Param("now") Instant now);
}
