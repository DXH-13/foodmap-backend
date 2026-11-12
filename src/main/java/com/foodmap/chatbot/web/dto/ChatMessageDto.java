package com.foodmap.chatbot.web.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Một tin nhắn trong lịch sử chat. Khớp schema {@code ChatMessage} trong hợp đồng. */
public record ChatMessageDto(
        UUID id,
        String role,
        String content,
        List<UUID> referencedPlaceIds,
        Instant createdAt
) {
}
