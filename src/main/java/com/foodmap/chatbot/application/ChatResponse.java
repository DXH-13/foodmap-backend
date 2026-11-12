package com.foodmap.chatbot.application;

import java.util.List;

/**
 * Kết quả của một lần gọi model không streaming.
 *
 * <p>Model có thể trả lời bằng văn bản ({@code text} khác null), yêu cầu gọi
 * tool ({@code functionCalls} không rỗng), hoặc cả hai — code gọi xử lý lần
 * lượt: thực thi tool trước, văn bản chỉ dùng khi không còn lời gọi hàm nào.
 */
public record ChatResponse(
        String text,
        List<FunctionCall> functionCalls
) {
}
