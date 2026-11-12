package com.foodmap.chatbot.infrastructure;

import com.foodmap.chatbot.api.ChatRole;
import com.foodmap.chatbot.domain.ChatMessage;
import com.foodmap.chatbot.domain.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    /** Toàn bộ tin nhắn của một phiên, cũ trước — để dựng lại lịch sử cho model. */
    List<ChatMessage> findBySessionOrderByCreatedAtAsc(ChatSession session);

    /** Số tin nhắn người dùng đã gửi từ một thời điểm — đếm cho giới hạn tốc độ. */
    @Query("""
            SELECT COUNT(m) FROM ChatMessage m
            WHERE m.session.userId = :userId
              AND m.role = :role
              AND m.createdAt > :since
            """)
    long countByUserAndRoleSince(@Param("userId") UUID userId,
                                 @Param("role") ChatRole role,
                                 @Param("since") Instant since);

    /** Tin nhắn cũ nhất của người dùng trong cửa sổ giới hạn — để tính phút chờ. */
    @Query("""
            SELECT MIN(m.createdAt) FROM ChatMessage m
            WHERE m.session.userId = :userId
              AND m.role = :role
              AND m.createdAt > :since
            """)
    Optional<Instant> oldestByUserAndRoleSince(@Param("userId") UUID userId,
                                               @Param("role") ChatRole role,
                                               @Param("since") Instant since);

    /** Xoá toàn bộ tin nhắn của một phiên (khi người dùng xoá lịch sử). */
    @Modifying
    @Query("DELETE FROM ChatMessage m WHERE m.session = :session")
    void deleteBySession(@Param("session") ChatSession session);
}
