package com.foodmap.chatbot.application;

/**
 * Lỗi khi gọi mô hình ngôn ngữ (hết hạn mức, lỗi mạng, lỗi 5xx, response sai
 * định dạng...). {@link ChatService} bắt lỗi này giữa luồng SSE và chuyển thành
 * sự kiện ERROR, thay vì để rơi thành lỗi 500.
 */
public class ChatModelException extends RuntimeException {

    public ChatModelException(String message) {
        super(message);
    }

    public ChatModelException(String message, Throwable cause) {
        super(message, cause);
    }
}
