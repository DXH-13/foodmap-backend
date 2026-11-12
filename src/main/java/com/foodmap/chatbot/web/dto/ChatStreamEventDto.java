package com.foodmap.chatbot.web.dto;

import com.foodmap.common.ApiError;

import java.util.List;
import java.util.UUID;

/**
 * Một sự kiện trong luồng SSE. Khớp schema {@code ChatStreamEvent} trong hợp đồng:
 * {@code TEXT_DELTA} (kèm {@code text}), {@code PLACE_REFERENCES} (kèm
 * {@code placeIds} để client render thẻ bấm mở), {@code DONE} (kèm
 * {@code messageId} của tin nhắn trợ lý đã lưu), {@code ERROR} (kèm {@code error}).
 */
public record ChatStreamEventDto(
        String type,
        String text,
        List<UUID> placeIds,
        UUID messageId,
        ApiError error
) {

    public static ChatStreamEventDto textDelta(String text) {
        return new ChatStreamEventDto("TEXT_DELTA", text, null, null, null);
    }

    public static ChatStreamEventDto placeReferences(List<UUID> placeIds) {
        return new ChatStreamEventDto("PLACE_REFERENCES", null, placeIds, null, null);
    }

    public static ChatStreamEventDto done(UUID messageId) {
        return new ChatStreamEventDto("DONE", null, null, messageId, null);
    }

    public static ChatStreamEventDto error(ApiError error) {
        return new ChatStreamEventDto("ERROR", null, null, null, error);
    }
}
