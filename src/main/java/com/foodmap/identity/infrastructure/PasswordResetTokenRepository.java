package com.foodmap.identity.infrastructure;

import com.foodmap.identity.domain.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Truy vấn bảng {@code password_reset_tokens}. Chỉ lưu SHA-256 hash — xem
 * {@link com.foodmap.identity.domain.TokenGenerator}.
 */
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByTokenHashAndUsedAtIsNull(String tokenHash);

    /**
     * Vô hiệu các token đặt lại mật khẩu chưa dùng của một người dùng — mỗi lần yêu cầu
     * mới thì các liên kết cũ không còn giá trị.
     */
    @Modifying
    @Query("""
            UPDATE PasswordResetToken t SET t.usedAt = :now
            WHERE t.user.id = :userId AND t.usedAt IS NULL
            """)
    void invalidateUnusedByUserId(@Param("userId") UUID userId, @Param("now") Instant now);
}
