package com.foodmap.chatbot.web.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Phiên chat hiện tại và lịch sử tin nhắn. Khớp schema {@code ChatSession} trong hợp đồng. */
public record ChatSessionDto(
        UUID id,
        List<ChatMessageDto> messages,
        Instant createdAt
) {
}
