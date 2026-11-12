package com.foodmap.chatbot.integration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodmap.chatbot.application.ChatModel;
import com.foodmap.chatbot.application.ChatModelException;
import com.foodmap.chatbot.application.ChatPart;
import com.foodmap.chatbot.application.ChatRequest;
import com.foodmap.chatbot.application.ChatResponse;
import com.foodmap.chatbot.application.FunctionCall;
import com.foodmap.chatbot.application.ToolDefinition;
import com.foodmap.chatbot.ChatProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Adapter gọi Gemini qua REST API (không dùng SDK riêng), cùng kiểu gọi như
 * dự án ClassTikTok dùng {@code fetch}: key gửi kèm query {@code ?key=}.
 *
 * <p>Hai endpoint dùng tới:
 * <ul>
 *   <li>{@code :generateContent} — gọi một lần, có kèm tool, để model quyết định
 *       gọi hàm hay trả lời bằng chữ.</li>
 *   <li>{@code :streamGenerateContent?alt=sse} — stream câu trả lời cuối sau khi
 *       xong các vòng tool.</li>
 * </ul>
 *
 * <p>Lỗi 429/5xx được thử lại tối đa 1 lần (tôn trọng header {@code Retry-After},
 * chờ tối đa 5 giây); lỗi khác hoặc vẫn lỗi sau lần thử lại thì ném
 * {@link ChatModelException} để service chuyển thành sự kiện ERROR.
 */
@Component
public class GeminiChatModel implements ChatModel {

    private static final Logger log = LoggerFactory.getLogger(GeminiChatModel.class);

    /** Số lần thử tối đa cho một lần gọi (lần đầu + 1 lần thử lại). */
    private static final int MAX_ATTEMPTS = 2;

    /** Thời gian chờ tối đa cho một lần thử lại (tránh treo luồng SSE quá lâu). */
    private static final long MAX_RETRY_WAIT_SECONDS = 5;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final ChatProperties chat;

    /**
     * Constructor cho test: nhận sẵn một {@link RestClient} (ví dụ dùng
     * {@code ClientHttpRequestFactory} giả lập) thay vì tự build từ builder.
     * Sandbox không cho Java kết nối loopback tới server giả của chính nó
     * (socket bị reset), nên test adapter không thể dùng HttpServer thật.
     */
    GeminiChatModel(RestClient restClient, ObjectMapper objectMapper, ChatProperties chat) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.chat = chat;
    }

    @Autowired
    public GeminiChatModel(RestClient.Builder restClientBuilder,
                           ObjectMapper objectMapper,
                           ChatProperties properties) {
        this.objectMapper = objectMapper;
        this.chat = properties;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(10));
        requestFactory.setReadTimeout(Duration.ofSeconds(120));
        this.restClient = restClientBuilder
                .baseUrl(chat.baseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public boolean isConfigured() {
        return chat.apiKey() != null && !chat.apiKey().isBlank();
    }

    @Override
    public ChatResponse generate(ChatRequest request) {
        String body = toJson(buildRequestBody(request, true));
        String json = postJson(":generateContent", body);
        return parseGenerateResponse(json);
    }

    @Override
    public void stream(ChatRequest request, Consumer<String> onTextDelta) {
        String body = toJson(buildRequestBody(request, false));
        postStream(":streamGenerateContent", body, onTextDelta);
    }

    // ── Gọi HTTP ──────────────────────────────────────────────────────────

    private String postJson(String action, String body) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return restClient.post()
                        .uri("/models/{model}" + action + "?key={key}", chat.model(), chat.apiKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body)
                        .exchange((req, res) -> {
                            if (!res.getStatusCode().is2xxSuccessful()) {
                                throw new GeminiHttpException(
                                        res.getStatusCode().value(),
                                        readBody(res),
                                        retryAfterSeconds(res));
                            }
                            return readBody(res);
                        }, true);
            } catch (GeminiHttpException ex) {
                if (ex.retryable() && attempt < MAX_ATTEMPTS) {
                    log.warn("Gemini HTTP {} (lan {}), thu lai sau {}s", ex.status, attempt, ex.waitSeconds());
                    sleepQuietly(ex.waitSeconds());
                    continue;
                }
                throw new ChatModelException("Gemini tra ve HTTP " + ex.status, ex);
            } catch (ResourceAccessException ex) {
                if (attempt < MAX_ATTEMPTS) {
                    log.warn("Khong ket noi duoc toi Gemini (lan {}), thu lai", attempt);
                    sleepQuietly(1);
                    continue;
                }
                throw new ChatModelException("Khong ket noi duoc toi Gemini", ex);
            }
        }
        throw new ChatModelException("Khong goi duoc Gemini");
    }

    private void postStream(String action, String body, Consumer<String> onTextDelta) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                restClient.post()
                        .uri("/models/{model}" + action + "?alt=sse&key={key}", chat.model(), chat.apiKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body)
                        .exchange((req, res) -> {
                            if (!res.getStatusCode().is2xxSuccessful()) {
                                throw new GeminiHttpException(
                                        res.getStatusCode().value(),
                                        readBody(res),
                                        retryAfterSeconds(res));
                            }
                            readSse(res, onTextDelta);
                            return null;
                        }, true);
                return;
            } catch (GeminiHttpException ex) {
                if (ex.retryable() && attempt < MAX_ATTEMPTS) {
                    log.warn("Gemini stream HTTP {} (lan {}), thu lai sau {}s", ex.status, attempt, ex.waitSeconds());
                    sleepQuietly(ex.waitSeconds());
                    continue;
                }
                throw new ChatModelException("Gemini stream tra ve HTTP " + ex.status, ex);
            } catch (ResourceAccessException ex) {
                if (attempt < MAX_ATTEMPTS) {
                    log.warn("Khong ket noi duoc toi Gemini stream (lan {}), thu lai", attempt);
                    sleepQuietly(1);
                    continue;
                }
                throw new ChatModelException("Khong ket noi duoc toi Gemini", ex);
            }
        }
        throw new ChatModelException("Khong goi duoc Gemini");
    }

    /** Đọc từng dòng SSE ({@code data: {...}}) và đẩy đoạn text về ngay khi có. */
    private void readSse(org.springframework.http.client.ClientHttpResponse response,
                         Consumer<String> onTextDelta) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(response.getBody(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("data:")) {
                    continue;
                }
                String data = line.substring("data:".length()).trim();
                if (data.isEmpty() || data.equals("[DONE]")) {
                    continue;
                }
                for (String text : extractTexts(data)) {
                    if (!text.isEmpty()) {
                        onTextDelta.accept(text);
                    }
                }
            }
        } catch (IOException ex) {
            throw new ChatModelException("Loi khi doc luong SSE tu Gemini", ex);
        }
    }

    /** Trích mọi đoạn text trong một chunk SSE (một response có thể có nhiều part). */
    private List<String> extractTexts(String json) {
        List<String> texts = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(json);
            for (JsonNode candidate : root.path("candidates")) {
                for (JsonNode part : candidate.path("content").path("parts")) {
                    if (part.has("text")) {
                        texts.add(part.path("text").asText(""));
                    }
                }
            }
        } catch (JsonProcessingException ex) {
            throw new ChatModelException("Gemini tra ve chunk SSE sai dinh dang", ex);
        }
        return texts;
    }

    // ── Dựng và đọc JSON ───────────────────────────────────────────────────

    private Map<String, Object> buildRequestBody(ChatRequest request, boolean withTools) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("system_instruction", Map.of("parts", List.of(Map.of("text", request.systemPrompt()))));
        List<Map<String, Object>> contents = new ArrayList<>();
        for (ChatRequest.ChatTurn turn : request.history()) {
            List<Map<String, Object>> parts = new ArrayList<>();
            for (ChatPart part : turn.parts()) {
                parts.add(toJsonPart(part));
            }
            contents.add(Map.of("role", turn.role(), "parts", parts));
        }
        body.put("contents", contents);
        if (withTools && !request.tools().isEmpty()) {
            List<Map<String, Object>> declarations = new ArrayList<>();
            for (ToolDefinition tool : request.tools()) {
                declarations.add(Map.of(
                        "name", tool.name(),
                        "description", tool.description(),
                        "parameters", tool.parameters()));
            }
            body.put("tools", List.of(Map.of("functionDeclarations", declarations)));
        }
        // Nhiệt độ thấp để model bám sát kết quả tool, ít bịa.
        body.put("generationConfig", Map.of("temperature", 0.3, "maxOutputTokens", 1024));
        return body;
    }

    private Map<String, Object> toJsonPart(ChatPart part) {
        if (part instanceof ChatPart.TextPart text) {
            return Map.of("text", text.text());
        }
        if (part instanceof ChatPart.FunctionCallPart call) {
            return Map.of("functionCall", Map.of("name", call.name(), "args", call.args()));
        }
        if (part instanceof ChatPart.FunctionResponsePart response) {
            return Map.of("functionResponse", Map.of("name", response.name(), "response", response.response()));
        }
        throw new ChatModelException("Loai ChatPart khong ho tro: " + part.getClass());
    }

    private ChatResponse parseGenerateResponse(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode candidate = root.path("candidates").path(0);
            if (candidate.isMissingNode()) {
                throw new ChatModelException("Gemini tra ve response khong co candidate: " + json);
            }
            StringBuilder text = new StringBuilder();
            List<FunctionCall> calls = new ArrayList<>();
            for (JsonNode part : candidate.path("content").path("parts")) {
                if (part.has("text")) {
                    text.append(part.path("text").asText(""));
                }
                if (part.has("functionCall")) {
                    JsonNode call = part.path("functionCall");
                    Map<String, Object> args = Map.of();
                    if (call.has("args") && call.path("args").isObject()) {
                        args = objectMapper.convertValue(call.path("args"), Map.class);
                    }
                    calls.add(new FunctionCall(call.path("name").asText(""), args));
                }
            }
            return new ChatResponse(text.isEmpty() ? null : text.toString(), calls);
        } catch (JsonProcessingException ex) {
            throw new ChatModelException("Gemini tra ve JSON sai dinh dang", ex);
        }
    }

    // ── Tiện ích ──────────────────────────────────────────────────────────

    private String toJson(Map<String, Object> body) {
        try {
            return objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException ex) {
            throw new ChatModelException("Loi khi tao request JSON cho Gemini", ex);
        }
    }

    private static String readBody(org.springframework.http.client.ClientHttpResponse response) {
        try {
            return new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            return "";
        }
    }

    private static long retryAfterSeconds(org.springframework.http.client.ClientHttpResponse response) {
        String header = response.getHeaders().getFirst("Retry-After");
        if (header == null) {
            return 1;
        }
        try {
            return Math.clamp(Long.parseLong(header.trim()), 0, MAX_RETRY_WAIT_SECONDS);
        } catch (NumberFormatException ex) {
            return 1;
        }
    }

    private static void sleepQuietly(long seconds) {
        try {
            Thread.sleep(Duration.ofSeconds(seconds));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    /** Lỗi HTTP từ Gemini, mang theo mã trạng thái và thời gian chờ thử lại. */
    private static final class GeminiHttpException extends RuntimeException {

        private final int status;
        private final long retryAfter;

        GeminiHttpException(int status, String body, long retryAfterSeconds) {
            super("HTTP " + status + ": " + body);
            this.status = status;
            this.retryAfter = retryAfterSeconds;
        }

        boolean retryable() {
            return status == 429 || status >= 500;
        }

        long waitSeconds() {
            return retryAfter;
        }
    }
}
