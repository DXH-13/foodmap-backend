package com.foodmap.chatbot.web;

import com.foodmap.chatbot.application.ChatModelException;
import com.foodmap.chatbot.application.ChatService;
import com.foodmap.chatbot.web.dto.ChatSessionDto;
import com.foodmap.chatbot.web.dto.ChatStreamEventDto;
import com.foodmap.chatbot.web.dto.SendChatMessageRequest;
import com.foodmap.identity.api.AuthPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Locale;

/**
 * Chatbot gợi ý quán (Phase 5, Bước 5). Ba endpoint khớp đúng hợp đồng
 * {@code docs/SDD/api/openapi.yaml}.
 *
 * <p>POST /messages trả về SSE: controller kiểm tra 503/429 đồng bộ trước khi
 * mở luồng (để hai lỗi này trả về dạng HTTP thường), rồi chạy xử lý trên một
 * virtual thread — luồng chính được giải phóng ngay, client nhận từng đoạn
 * TEXT_DELTA theo thời gian thực.
 */
@RestController
@RequestMapping("/app/v1/chat")
@Tag(name = "chat", description = "Chatbot gợi ý quán")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    /** Thời gian tối đa của một luồng SSE (ms) — đủ cho model trả lời dài. */
    private static final long SSE_TIMEOUT_MILLIS = 120_000L;

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping(value = "/messages", produces = "text/event-stream")
    @Operation(operationId = "sendChatMessage", summary = "Gửi tin nhắn cho chatbot")
    public SseEmitter sendChatMessage(@AuthenticationPrincipal AuthPrincipal principal,
                                      @Valid @RequestBody SendChatMessageRequest request,
                                      Locale locale) {
        // 503 (chưa có key Gemini) và 429 (vượt giới hạn) phải trả dạng HTTP
        // thường — kiểm tra trước khi mở emitter, vì sau đó đã là luồng 200.
        chatService.preflight(principal.userId());
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MILLIS);
        emitter.onTimeout(() -> log.warn("SSE chat het thoi gian cho user {}", principal.userId()));
        Thread.ofVirtual().start(() -> {
            try {
                chatService.handleMessage(principal.userId(), request, locale,
                        event -> send(emitter, event));
                emitter.complete();
            } catch (Exception ex) {
                log.warn("SSE chat loi cho user {}: {}", principal.userId(), ex.getMessage());
                emitter.completeWithError(ex);
            }
        });
        return emitter;
    }

    private static void send(SseEmitter emitter, ChatStreamEventDto event) {
        try {
            emitter.send(event, MediaType.APPLICATION_JSON);
        } catch (IOException ex) {
            // Client đã ngắt kết nối — dừng luồng, service vẫn lưu phần đã trả lời.
            throw new ChatModelException("Client da ngat ket noi SSE", ex);
        }
    }

    @GetMapping("/sessions/current")
    @Operation(operationId = "getCurrentChatSession", summary = "Lấy lịch sử phiên chat hiện tại")
    public ChatSessionDto getCurrentChatSession(@AuthenticationPrincipal AuthPrincipal principal) {
        return chatService.getCurrentSession(principal.userId());
    }

    @DeleteMapping("/sessions/current")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(operationId = "clearChatSession", summary = "Xoá lịch sử phiên chat hiện tại")
    public void clearChatSession(@AuthenticationPrincipal AuthPrincipal principal) {
        chatService.clearSession(principal.userId());
    }
}
