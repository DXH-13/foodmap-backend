package com.foodmap.chatbot.application;

import java.util.Map;

/**
 * Một phần nội dung trong một lượt hội thoại gửi cho mô hình ngôn ngữ.
 *
 * <p>Hình dạng khớp với API của Gemini (và của hầu hết LLM có function calling):
 * văn bản thuần, lời gọi hàm, hoặc kết quả trả về của một lời gọi hàm.
 */
public sealed interface ChatPart
        permits ChatPart.TextPart, ChatPart.FunctionCallPart, ChatPart.FunctionResponsePart {

    /** Văn bản thuần của người dùng hoặc của model. */
    record TextPart(String text) implements ChatPart {
    }

    /** Model yêu cầu gọi một tool nội bộ. */
    record FunctionCallPart(String name, Map<String, Object> args) implements ChatPart {
    }

    /** Kết quả của một tool, gửi lại cho model để nó trả lời tiếp. */
    record FunctionResponsePart(String name, Map<String, Object> response) implements ChatPart {
    }
}
