package com.foodmap.chatbot.domain;

import com.foodmap.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * Một phiên chat của một người dùng. Mỗi người chỉ có một phiên hiện tại —
 * tạo ở tin nhắn đầu tiên, xoá khi người dùng xoá lịch sử.
 *
 * <p>Chỉ tham chiếu tới người dùng bằng mã định danh, không giữ tham chiếu
 * đối tượng sang entity của module {@code identity}.
 */
@Entity
@Table(name = "chat_sessions", schema = "chatbot")
public class ChatSession extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    protected ChatSession() {
    }

    public ChatSession(UUID userId) {
        this.userId = userId;
    }

    public UUID getUserId() {
        return userId;
    }
}
