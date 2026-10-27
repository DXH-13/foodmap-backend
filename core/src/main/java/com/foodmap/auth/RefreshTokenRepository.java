package com.foodmap.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Thu hồi có điều kiện. Trả về 0 nghĩa là token đã bị thu hồi trước đó — kể cả bởi
     * một request khác chạy song song chỉ vài mili giây trước. Đọc rồi mới ghi
     * ({@code if (revokedAt == null) ...}) sẽ để lọt trường hợp đó.
     */
    @Modifying
    @Query("UPDATE RefreshToken t SET t.revokedAt = :now WHERE t.id = :id AND t.revokedAt IS NULL")
    int revokeIfActive(@Param("id") UUID id, @Param("now") Instant now);

    @Modifying
    @Query("UPDATE RefreshToken t SET t.revokedAt = :now WHERE t.userId = :userId AND t.revokedAt IS NULL")
    int revokeAllActive(@Param("userId") UUID userId, @Param("now") Instant now);
}
