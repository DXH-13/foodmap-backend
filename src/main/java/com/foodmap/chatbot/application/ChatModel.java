package com.foodmap.chatbot.application;

import java.util.function.Consumer;

/**
 * Cổng (port) tới mô hình ngôn ngữ. Code nghiệp vụ chỉ biết giao diện này —
 * không biết đang gọi Gemini, Claude hay bất kỳ nhà cung cấp nào khác, cũng
 * không biết key hay địa chỉ API. Adapter cụ thể nằm ở {@code integration/}
 * (hiện tại: {@code GeminiChatModel}).
 *
 * <p>Đổi nhà cung cấp sau này chỉ là thay adapter, không sửa module nghiệp vụ
 * (xem kiến trúc đích, mục "Tích hợp với hệ thống bên ngoài").
 */
public interface ChatModel {

    /** Đã có key cấu hình hay chưa — chưa có thì service trả 503, không gọi. */
    boolean isConfigured();

    /**
     * Một lần gọi model không streaming, có thể kèm tool. Dùng cho các vòng
     * gọi tool: model trả lời bằng lời gọi hàm thì service thực thi rồi gọi tiếp.
     */
    ChatResponse generate(ChatRequest request);

    /**
     * Gọi model ở chế độ streaming và đẩy từng đoạn văn bản về qua
     * {@code onTextDelta} ngay khi nhận được — dùng để phát sự kiện SSE
     * {@code TEXT_DELTA} cho client theo thời gian thực.
     *
     * <p>Chỉ gọi sau khi đã xong các vòng tool (request không kèm tool), để câu
     * trả lời cuối được stream thay vì chờ nguyên một cục.
     */
    void stream(ChatRequest request, Consumer<String> onTextDelta);
}
