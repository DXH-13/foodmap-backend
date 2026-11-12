package com.foodmap.chatbot.application;

import java.util.List;

/**
 * Một yêu cầu gửi cho mô hình ngôn ngữ.
 *
 * @param systemPrompt hướng dẫn hệ thống (ngôn ngữ theo locale của người dùng).
 * @param history      các lượt hội thoại trước đó + tin nhắn hiện tại.
 *                     Vai trò là {@code "user"} hoặc {@code "model"} (cách gọi
 *                     của API Gemini; "model" tương đương "assistant" trong DB).
 * @param tools        danh sách tool model được phép gọi; rỗng thì model chỉ
 *                     được trả lời bằng văn bản.
 */
public record ChatRequest(
        String systemPrompt,
        List<ChatTurn> history,
        List<ToolDefinition> tools
) {

    public record ChatTurn(String role, List<ChatPart> parts) {
    }
}
