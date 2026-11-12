package com.foodmap.chatbot.api;

/**
 * Vai trò của một tin nhắn trong phiên chat. Khớp enum {@code ChatRole} trong hợp
 * đồng và CHECK trên bảng {@code chat_messages}.
 *
 * <p>Đặt ở {@code api/} vì nó xuất hiện trong JSON trả ra — cùng tiền lệ
 * {@code UserRole} (Bước 3) và {@code ReviewStatus} (Bước 4).
 */
public enum ChatRole {
    USER,
    ASSISTANT
}
