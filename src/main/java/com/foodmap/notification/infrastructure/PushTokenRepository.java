package com.foodmap.notification.infrastructure;

import com.foodmap.notification.api.Platform;
import com.foodmap.notification.domain.PushToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Truy vấn bảng {@code push_tokens} — chỉ module {@code notification} dùng. */
public interface PushTokenRepository extends JpaRepository<PushToken, UUID> {

    List<PushToken> findByUserId(UUID userId);

    Optional<PushToken> findByUserIdAndTokenAndPlatform(UUID userId, String token, Platform platform);
}
