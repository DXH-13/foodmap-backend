package com.foodmap.chatbot;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Cấu hình chatbot, thuộc về module {@code chatbot}.
 *
 * <p>Trước đây nhóm này nằm trong {@code FoodmapProperties.Chat} ở
 * {@code config.props} — một package con nội bộ của module {@code config},
 * nên {@code ChatService}/{@code GeminiChatModel} (module {@code chatbot})
 * dùng nó là vi phạm ranh giới Modulith ("depends on non-exposed type").
 * Đưa về đây thì {@code chatbot} tự sở hữu cấu hình của mình; tiền tố YAML
 * giữ nguyên {@code foodmap.chat.*} nên {@code application.yml} không cần đổi.
 *
 * <p>Khóa Gemini lấy từ biến môi trường {@code GEMINI_API_KEY} (mặc định rỗng
 * → chatbot trả 503, xem {@code ChatService#preflight}); model mặc định
 * {@code gemini-2.0-flash} (miễn phí). Không bao giờ ghi key thật vào repo.
 */
@Validated
@ConfigurationProperties(prefix = "foodmap.chat")
public record ChatProperties(
        String apiKey,
        String model,
        String baseUrl,
        @Min(1) int maxMessagesPerHour
) {
}
