package com.foodmap.chatbot.infrastructure;

import com.foodmap.chatbot.domain.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ChatSessionRepository extends JpaRepository<ChatSession, UUID> {

    /** Phiên hiện tại của một người dùng — mới nhất nếu lỡ có nhiều hơn một. */
    Optional<ChatSession> findFirstByUserIdOrderByCreatedAtDesc(UUID userId);
}
