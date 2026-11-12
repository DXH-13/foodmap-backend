package com.foodmap.chatbot.domain;

import com.foodmap.chatbot.api.ChatRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Một tin nhắn trong phiên chat.
 *
 * <p>Không extends {@code BaseEntity}: bảng {@code chat_messages} không có cột
 * {@code updated_at} nên entity này tự khai {@code id} và {@code created_at}
 * (cùng lý do như {@code Favorite} ở Bước 4). {@code referencedPlaceIds} là
 * danh sách id các địa điểm được nhắc tới trong câu trả lời, để client render
 * thẻ bấm mở — lưu JSONB, giống {@code Notification.payload} ở Bước 5.
 */
@Entity
@Table(name = "chat_messages")
@EntityListeners(AuditingEntityListener.class)
public class ChatMessage {

    @Id
    @GeneratedValue
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_session_id", nullable = false)
    private ChatSession session;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ChatRole role;

    @Column(nullable = false)
    private String content;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "referenced_place_ids", columnDefinition = "jsonb", nullable = false)
    private List<UUID> referencedPlaceIds = List.of();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ChatMessage() {
    }

    public ChatMessage(ChatSession session, ChatRole role, String content, List<UUID> referencedPlaceIds) {
        this.session = session;
        this.role = role;
        this.content = content;
        this.referencedPlaceIds = referencedPlaceIds == null ? List.of() : List.copyOf(referencedPlaceIds);
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public ChatSession getSession() {
        return session;
    }

    public ChatRole getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }

    public List<UUID> getReferencedPlaceIds() {
        return referencedPlaceIds;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
