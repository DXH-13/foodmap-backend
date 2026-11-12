package com.foodmap.chatbot.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Tin nhắn gửi cho chatbot. Khớp schema {@code SendChatMessageRequest} trong hợp đồng.
 */
public record SendChatMessageRequest(
        @NotBlank String message,

        /** Vị trí hiện tại, để chatbot gợi ý quán quanh đây — không bắt buộc. */
        @Valid CoordinatesDto coordinates,

        /**
         * Ngôn ngữ trả lời (vi/en). Bỏ trống thì suy ra từ câu hỏi, rồi từ header
         * Accept-Language — xem {@code ChatService#resolveLocale}.
         */
        @Pattern(regexp = "vi|en") String locale
) {

    /** Tọa độ. Khai riêng ở chatbot thay vì dùng lại của module place — module
     * khác không được import vào {@code web/} của place (ranh giới Modulith). */
    public record CoordinatesDto(
            @Min(-90) @Max(90) double latitude,
            @Min(-180) @Max(180) double longitude
    ) {
    }
}
