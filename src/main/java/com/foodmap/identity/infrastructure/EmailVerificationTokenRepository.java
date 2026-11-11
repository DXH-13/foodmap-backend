package com.foodmap.identity.infrastructure;

import com.foodmap.identity.domain.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Truy vấn bảng {@code email_verification_tokens}. Chỉ lưu SHA-256 hash — xem
 * {@link com.foodmap.identity.domain.TokenGenerator}.
 */
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, UUID> {

    Optional<EmailVerificationToken> findByTokenHashAndUsedAtIsNull(String tokenHash);

    /**
     * Vô hiệu các token xác minh chưa dùng của một người dùng — mỗi lần gửi lại thì
     * các mã cũ không còn giá trị.
     */
    @Modifying
    @Query("""
            UPDATE EmailVerificationToken t SET t.usedAt = :now
            WHERE t.user.id = :userId AND t.usedAt IS NULL
            """)
    void invalidateUnusedByUserId(@Param("userId") UUID userId, @Param("now") Instant now);
}
