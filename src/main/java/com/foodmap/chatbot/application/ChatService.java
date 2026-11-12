package com.foodmap.chatbot.application;

import com.foodmap.chatbot.api.ChatRole;
import com.foodmap.chatbot.domain.ChatMessage;
import com.foodmap.chatbot.domain.ChatSession;
import com.foodmap.chatbot.infrastructure.ChatMessageRepository;
import com.foodmap.chatbot.infrastructure.ChatSessionRepository;
import com.foodmap.chatbot.web.dto.ChatMessageDto;
import com.foodmap.chatbot.web.dto.ChatSessionDto;
import com.foodmap.chatbot.web.dto.ChatStreamEventDto;
import com.foodmap.chatbot.web.dto.SendChatMessageRequest;
import com.foodmap.common.ApiError;
import com.foodmap.common.ServiceUnavailableException;
import com.foodmap.common.TooManyRequestsException;
import com.foodmap.chatbot.ChatProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Luồng chat gợi ý quán: phiên, giới hạn tốc độ, gọi model kèm tool, stream
 * câu trả lời về cho client, và lưu lịch sử.
 *
 * <p>Thứ tự quan trọng, đọc trước khi sửa:
 * <ol>
 *   <li>{@link #preflight(UUID)} chạy đồng bộ <b>trước</b> khi controller tạo
 *       {@code SseEmitter} — 503 (chưa có key) và 429 (vượt giới hạn) phải trả
 *       về dạng HTTP thường, không phải sự kiện SSE.</li>
 *   <li>Tin nhắn của người dùng được lưu <b>trước</b> khi gọi model — model lỗi
 *       giữa chừng cũng không mất tin nhắn.</li>
 *   <li>Câu trả lời của trợ lý được lưu <b>sau</b> khi stream xong, kể cả khi
 *       stream lỗi giữa chừng (lưu phần đã trả lời được).</li>
 * </ol>
 *
 * <p>Không có chỉ mục tìm kiếm riêng: tool gọi {@code PlaceApi} trực tiếp lúc
 * hỏi nên dữ liệu luôn mới — vì vậy module này <b>không</b> nghe các sự kiện
 * {@code PlaceCreated}/{@code PlaceUpdated}/{@code PlaceVisibilityChanged}.
 */
@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    /** Số vòng gọi tool tối đa trong một lượt chat. */
    private static final int MAX_TOOL_ROUNDS = 3;

    /** Số tin nhắn lịch sử tối đa đưa cho model (cũ hơn thì bỏ). */
    private static final int HISTORY_MESSAGES = 20;

    /** Độ dài mỗi đoạn TEXT_DELTA khi phát lại văn bản đã có sẵn. */
    private static final int TEXT_CHUNK_SIZE = 120;

    private final ChatSessionRepository sessions;
    private final ChatMessageRepository messages;
    private final ChatModel chatModel;
    private final ChatTools chatTools;
    private final ChatProperties properties;
    private final MessageSource messageSource;

    public ChatService(ChatSessionRepository sessions,
                       ChatMessageRepository messages,
                       ChatModel chatModel,
                       ChatTools chatTools,
                       ChatProperties properties,
                       MessageSource messageSource) {
        this.sessions = sessions;
        this.messages = messages;
        this.chatModel = chatModel;
        this.chatTools = chatTools;
        this.properties = properties;
        this.messageSource = messageSource;
    }

    /**
     * Kiểm tra trước khi mở luồng SSE: chưa cấu hình key Gemini thì 503, vượt
     * giới hạn 30 tin nhắn / người / giờ thì 429.
     */
    public void preflight(UUID userId) {
        if (!chatModel.isConfigured()) {
            throw new ServiceUnavailableException("AI_UNAVAILABLE", "chat.error.service_unavailable");
        }
        int max = properties.maxMessagesPerHour();
        Instant since = Instant.now().minus(1, ChronoUnit.HOURS);
        long count = messages.countByUserAndRoleSince(userId, ChatRole.USER, since);
        if (count >= max) {
            long minutes = messages.oldestByUserAndRoleSince(userId, ChatRole.USER, since)
                    .map(oldest -> 60 - Duration.between(oldest, Instant.now()).toMinutes())
                    .orElse(60L);
            throw new TooManyRequestsException("RATE_LIMITED", "chat.error.rate_limited",
                    Math.max(1, minutes));
        }
    }

    /** Lịch sử phiên hiện tại; chưa có phiên thì tạo phiên trống mới. */
    @Transactional
    public ChatSessionDto getCurrentSession(UUID userId) {
        ChatSession session = getOrCreateSession(userId);
        return toDto(session, messages.findBySessionOrderByCreatedAtAsc(session));
    }

    /** Xoá lịch sử phiên hiện tại (tin nhắn + phiên). */
    @Transactional
    public void clearSession(UUID userId) {
        sessions.findFirstByUserIdOrderByCreatedAtDesc(userId).ifPresent(session -> {
            messages.deleteBySession(session);
            sessions.delete(session);
        });
    }

    /**
     * Xử lý một tin nhắn: lưu tin nhắn người dùng, chạy các vòng gọi tool, stream
     * câu trả lời cuối về qua {@code sink} (mỗi sự kiện là một
     * {@link ChatStreamEventDto}), rồi lưu câu trả lời và phát sự kiện DONE.
     *
     * <p>Lỗi model giữa chừng không ném ra ngoài: phần trả lời được vẫn lưu, và
     * client nhận một sự kiện ERROR để hiển thị.
     */
    public void handleMessage(UUID userId, SendChatMessageRequest request, Locale acceptLocale,
                              Consumer<ChatStreamEventDto> sink) {
        String lang = resolveLocale(request, acceptLocale);
        Locale locale = Locale.forLanguageTag(lang);
        ChatTools.ToolContext context = new ChatTools.ToolContext(
                request.coordinates() == null ? null : request.coordinates().latitude(),
                request.coordinates() == null ? null : request.coordinates().longitude(),
                lang);

        ChatSession session = getOrCreateSession(userId);
        // Dựng lịch sử TRƯỚC khi lưu tin nhắn mới — nếu lưu trước rồi mới đọc
        // lại lịch sử thì tin nhắn hiện tại sẽ xuất hiện hai lần.
        List<ChatRequest.ChatTurn> turns = buildHistory(session);
        saveMessage(session, ChatRole.USER, request.message(), List.of());
        turns.add(new ChatRequest.ChatTurn("user", List.of(new ChatPart.TextPart(request.message()))));
        String systemPrompt = buildSystemPrompt(locale);
        List<UUID> referencedIds = new ArrayList<>();
        StringBuilder fullText = new StringBuilder();
        try {
            String finalText = runToolLoop(systemPrompt, turns, context, referencedIds, fullText, sink);
            if (finalText == null) {
                // Model vẫn đòi gọi tool sau số vòng tối đa — xin câu trả lời cuối
                // bằng streaming, không kèm tool nữa.
                ChatRequest finalRequest = new ChatRequest(systemPrompt, turns, List.of());
                chatModel.stream(finalRequest, delta -> emitTextDelta(delta, fullText, sink));
                finalText = fullText.toString();
            } else if (!finalText.isBlank()) {
                // Văn bản đã có sẵn từ vòng generate — phát lại thành từng đoạn
                // cho giống luồng SSE thay vì dồn một cục.
                chunkText(finalText).forEach(chunk -> emitTextDelta(chunk, fullText, sink));
            }
            if (finalText == null || finalText.isBlank()) {
                throw new ChatModelException("Model tra ve cau tra loi rong.");
            }
            if (!referencedIds.isEmpty()) {
                sink.accept(ChatStreamEventDto.placeReferences(ChatTools.deduplicate(referencedIds)));
            }
            ChatMessage assistant = saveMessage(session, ChatRole.ASSISTANT,
                    fullText.toString(), ChatTools.deduplicate(referencedIds));
            sink.accept(ChatStreamEventDto.done(assistant.getId()));
        } catch (ChatModelException ex) {
            log.warn("Chat that bai cho user {}: {}", userId, ex.getMessage());
            saveMessage(session, ChatRole.ASSISTANT, fullText.toString(),
                    ChatTools.deduplicate(referencedIds));
            sink.accept(ChatStreamEventDto.error(ApiError.of(
                    "AI_UNAVAILABLE",
                    messageSource.getMessage("chat.error.service_unavailable", null, locale),
                    UUID.randomUUID().toString())));
        }
    }

    /**
     * Các vòng gọi tool: gọi model (kèm tool) → thực thi lời gọi hàm → gửi kết
     * quả lại cho model → lặp. Trả về văn bản cuối khi model trả lời bằng chữ,
     * hoặc {@code null} khi hết số vòng mà model vẫn đòi gọi tool.
     */
    private String runToolLoop(String systemPrompt, List<ChatRequest.ChatTurn> turns,
                               ChatTools.ToolContext context, List<UUID> referencedIds,
                               StringBuilder fullText, Consumer<ChatStreamEventDto> sink) {
        for (int round = 0; round < MAX_TOOL_ROUNDS; round++) {
            ChatResponse response = chatModel.generate(
                    new ChatRequest(systemPrompt, turns, chatTools.definitions()));
            if (response.functionCalls().isEmpty()) {
                return response.text();
            }
            List<ChatPart> callParts = new ArrayList<>();
            List<ChatPart> responseParts = new ArrayList<>();
            for (FunctionCall call : response.functionCalls()) {
                ChatTools.ToolOutcome outcome = chatTools.execute(call, context);
                referencedIds.addAll(outcome.placeIds());
                callParts.add(new ChatPart.FunctionCallPart(call.name(), call.args()));
                responseParts.add(new ChatPart.FunctionResponsePart(call.name(), outcome.response()));
            }
            turns.add(new ChatRequest.ChatTurn("model", callParts));
            turns.add(new ChatRequest.ChatTurn("user", responseParts));
        }
        return null;
    }

    private void emitTextDelta(String delta, StringBuilder fullText, Consumer<ChatStreamEventDto> sink) {
        if (delta == null || delta.isEmpty()) {
            return;
        }
        fullText.append(delta);
        sink.accept(ChatStreamEventDto.textDelta(delta));
    }

    private static List<String> chunkText(String text) {
        List<String> chunks = new ArrayList<>();
        for (int i = 0; i < text.length(); i += TEXT_CHUNK_SIZE) {
            chunks.add(text.substring(i, Math.min(text.length(), i + TEXT_CHUNK_SIZE)));
        }
        return chunks;
    }

    private ChatSession getOrCreateSession(UUID userId) {
        return sessions.findFirstByUserIdOrderByCreatedAtDesc(userId)
                .orElseGet(() -> sessions.save(new ChatSession(userId)));
    }

    private ChatMessage saveMessage(ChatSession session, ChatRole role, String content,
                                    List<UUID> referencedPlaceIds) {
        return messages.save(new ChatMessage(session, role, content, referencedPlaceIds));
    }

    private List<ChatRequest.ChatTurn> buildHistory(ChatSession session) {
        List<ChatMessage> history = messages.findBySessionOrderByCreatedAtAsc(session);
        int from = Math.max(0, history.size() - HISTORY_MESSAGES);
        List<ChatRequest.ChatTurn> turns = new ArrayList<>();
        for (ChatMessage message : history.subList(from, history.size())) {
            String role = message.getRole() == ChatRole.USER ? "user" : "model";
            turns.add(new ChatRequest.ChatTurn(role, List.of(new ChatPart.TextPart(message.getContent()))));
        }
        return turns;
    }

    /**
     * Ngôn ngữ trả lời: ưu tiên {@code locale} trong request, rồi suy ra từ câu
     * hỏi (tiếng Việt có dấu hoặc từ quen thuộc), cuối cùng là header
     * {@code Accept-Language}. Chỉ có vi/en.
     */
    private String resolveLocale(SendChatMessageRequest request, Locale acceptLocale) {
        if (request.locale() != null && (request.locale().equals("vi") || request.locale().equals("en"))) {
            return request.locale();
        }
        if (detectVietnamese(request.message())) {
            return "vi";
        }
        String language = acceptLocale == null ? "vi" : acceptLocale.getLanguage();
        return language.equals("en") ? "en" : "vi";
    }

    private static final String VIETNAMESE_CHARS =
            "àáảãạăắằẳẵặâấầẩẫậđèéẻẽẹêếềểễệìíỉĩịòóỏõọôốồổỗộơớờởỡợùúủũụưứừửữựỳýỷỹỵ";

    private static boolean detectVietnamese(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        for (int i = 0; i < lower.length(); i++) {
            if (VIETNAMESE_CHARS.indexOf(lower.charAt(i)) >= 0) {
                return true;
            }
        }
        return lower.contains("quán") || lower.contains("món") || lower.contains("gần đây")
                || lower.contains("tìm") || lower.contains("ngon") || lower.contains("phở")
                || lower.contains("bún") || lower.contains("cơm");
    }

    /**
     * Hướng dẫn hệ thống cho model. Điểm cốt lõi (yêu cầu trong hợp đồng):
     * chỉ gợi ý quán có trong kết quả tool, không bao giờ bịa tên quán, địa chỉ
     * hay số sao; không tìm thấy thì nói đúng câu đã dịch sẵn.
     */
    private String buildSystemPrompt(Locale locale) {
        String noResults = messageSource.getMessage("chat.message.no_results", null, locale);
        if (locale.getLanguage().equals("en")) {
            return """
                    You are the FoodMap assistant suggesting places to eat in Vietnam. Answer in English.
                    Mandatory rules:
                    - Only suggest places returned by the search_places / get_place_details / list_place_reviews tools. Never invent a place name, address, rating or review.
                    - Before suggesting any place, you MUST call search_places (use the user's coordinates when provided, otherwise the keywords from the question).
                    - If the tools return no places, reply with exactly this sentence and suggest nothing else: "%s"
                    - Mention places only by the exact names returned by the tools.
                    - Keep answers short and friendly.
                    """.formatted(noResults);
        }
        return """
                Bạn là trợ lý gợi ý quán ăn của FoodMap. Trả lời bằng tiếng Việt.
                Quy tắc bắt buộc:
                - Chỉ gợi ý địa điểm có trong kết quả trả về của tool search_places / get_place_details / list_place_reviews. Không bao giờ bịa tên quán, địa chỉ, số sao hay đánh giá.
                - Trước khi gợi ý quán, bắt buộc phải gọi tool search_places (dùng tọa độ người dùng khi có, nếu không thì dùng từ khóa trong câu hỏi).
                - Nếu tool không trả về địa điểm nào, trả lời đúng câu sau và không gợi ý thêm quán nào khác: "%s"
                - Chỉ nhắc tới quán bằng đúng tên tool trả về.
                - Trả lời ngắn gọn, thân thiện.
                """.formatted(noResults);
    }

    private ChatSessionDto toDto(ChatSession session, List<ChatMessage> history) {
        List<ChatMessageDto> messageDtos = history.stream()
                .map(message -> new ChatMessageDto(
                        message.getId(),
                        message.getRole().name(),
                        message.getContent(),
                        message.getReferencedPlaceIds(),
                        message.getCreatedAt()))
                .toList();
        return new ChatSessionDto(session.getId(), messageDtos, session.getCreatedAt());
    }
}
