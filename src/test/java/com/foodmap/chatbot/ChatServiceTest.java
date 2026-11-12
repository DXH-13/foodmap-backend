package com.foodmap.chatbot;

import com.foodmap.chatbot.api.ChatRole;
import com.foodmap.chatbot.application.ChatModel;
import com.foodmap.chatbot.application.ChatModelException;
import com.foodmap.chatbot.application.ChatPart.FunctionCallPart;
import com.foodmap.chatbot.application.ChatPart.FunctionResponsePart;
import com.foodmap.chatbot.application.ChatPart.TextPart;
import com.foodmap.chatbot.application.ChatRequest;
import com.foodmap.chatbot.application.ChatResponse;
import com.foodmap.chatbot.application.ChatService;
import com.foodmap.chatbot.application.ChatTools;
import com.foodmap.chatbot.application.FunctionCall;
import com.foodmap.chatbot.domain.ChatMessage;
import com.foodmap.chatbot.domain.ChatSession;
import com.foodmap.chatbot.infrastructure.ChatMessageRepository;
import com.foodmap.chatbot.infrastructure.ChatSessionRepository;
import com.foodmap.chatbot.web.dto.ChatStreamEventDto;
import com.foodmap.chatbot.web.dto.SendChatMessageRequest;
import com.foodmap.common.BusinessException;
import com.foodmap.common.ServiceUnavailableException;
import com.foodmap.common.TooManyRequestsException;
import com.foodmap.chatbot.ChatProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.MessageSource;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Kiểm thử {@link ChatService} với ChatModel bị mock — KHÔNG gọi Gemini thật,
 * không cần key thật, không cần DB.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChatServiceTest {

    @Mock
    ChatSessionRepository sessions;

    @Mock
    ChatMessageRepository messages;

    @Mock
    ChatModel chatModel;

    @Mock
    ChatTools chatTools;

    @Mock
    MessageSource messageSource;

    ChatProperties properties;

    ChatService service;

    UUID userId = UUID.randomUUID();
    ChatSession session;

    @BeforeEach
    void setUp() {
        properties = new ChatProperties("test-key", "gemini-2.0-flash",
                "https://generativelanguage.googleapis.com/v1beta", 30);
        service = new ChatService(sessions, messages, chatModel, chatTools, properties, messageSource);
        session = new ChatSession(userId);
        session.setId(UUID.randomUUID());
        when(sessions.findFirstByUserIdOrderByCreatedAtDesc(userId)).thenReturn(Optional.of(session));
        when(sessions.save(any(ChatSession.class))).thenAnswer(inv -> inv.getArgument(0));
        when(messages.save(any(ChatMessage.class))).thenAnswer(inv -> {
            ChatMessage m = inv.getArgument(0);
            if (m.getId() == null) {
                m.setId(UUID.randomUUID());
            }
            return m;
        });
        when(messages.findBySessionOrderByCreatedAtAsc(session)).thenReturn(List.of());
        // MessageSource trả về đúng key — đủ để kiểm tra luồng, không cần text thật.
        when(messageSource.getMessage(anyString(), any(), any(Locale.class)))
                .thenAnswer(inv -> inv.getArgument(0, String.class));
        when(messageSource.getMessage(anyString(), any(), anyString(), any(Locale.class)))
                .thenAnswer(inv -> inv.getArgument(0, String.class));
        when(chatModel.isConfigured()).thenReturn(true);
    }

    @Test
    void preflight_throws503_whenNoKeyConfigured() {
        when(chatModel.isConfigured()).thenReturn(false);
        assertThatThrownBy(() -> service.preflight(userId))
                .isInstanceOf(ServiceUnavailableException.class);
    }

    @Test
    void preflight_throws429_whenHourlyLimitReached() {
        when(messages.countByUserAndRoleSince(eq(userId), eq(ChatRole.USER), any(Instant.class)))
                .thenReturn(30L);
        assertThatThrownBy(() -> service.preflight(userId))
                .isInstanceOf(TooManyRequestsException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                        .isEqualTo("RATE_LIMITED"));
    }

    @Test
    void preflight_passes_whenUnderLimit() {
        when(messages.countByUserAndRoleSince(eq(userId), eq(ChatRole.USER), any(Instant.class)))
                .thenReturn(29L);
        service.preflight(userId); // không ném
    }

    @Test
    void handleMessage_toolCallFlow_emitsPlaceReferencesAndDone_andPersistsBothMessages() {
        UUID placeId = UUID.randomUUID();
        when(chatModel.generate(any(ChatRequest.class))).thenAnswer(inv -> {
            ChatRequest req = inv.getArgument(0);
            // Vòng 1: model gọi tool. Vòng 2: model trả lời văn bản.
            if (req.history().size() == 1) {
                return new ChatResponse(null, List.of(
                        new FunctionCall("search_places", Map.of("query", "phở", "limit", 5))));
            }
            return new ChatResponse("Đây là quán phở ngon nhất.", List.of());
        });
        when(chatTools.execute(any(FunctionCall.class), any()))
                .thenReturn(new ChatTools.ToolOutcome(
                        java.util.Map.of("places", List.of()), List.of(placeId)));

        List<ChatStreamEventDto> events = new ArrayList<>();
        service.handleMessage(userId,
                new SendChatMessageRequest("Tìm quán phở ngon", null, "vi"),
                Locale.of("vi"), events::add);

        // Tin USER được lưu trước khi gọi model.
        ArgumentCaptor<ChatMessage> saved = ArgumentCaptor.forClass(ChatMessage.class);
        verify(messages, org.mockito.Mockito.times(2)).save(saved.capture());
        assertThat(saved.getAllValues().get(0).getRole()).isEqualTo(ChatRole.USER);
        assertThat(saved.getAllValues().get(0).getContent()).isEqualTo("Tìm quán phở ngon");
        ChatMessage assistant = saved.getAllValues().get(1);
        assertThat(assistant.getRole()).isEqualTo(ChatRole.ASSISTANT);
        assertThat(assistant.getContent()).isEqualTo("Đây là quán phở ngon nhất.");
        assertThat(assistant.getReferencedPlaceIds()).containsExactly(placeId);

        assertThat(events).extracting(ChatStreamEventDto::type)
                .containsExactly("TEXT_DELTA", "PLACE_REFERENCES", "DONE");
        assertThat(events.get(1).placeIds()).containsExactly(placeId);
        assertThat(events.get(2).messageId()).isEqualTo(assistant.getId());
    }

    @Test
    void handleMessage_textOnly_noPlaceReferences() {
        when(chatModel.generate(any(ChatRequest.class)))
                .thenReturn(new ChatResponse("Xin chào!", List.of()));
        List<ChatStreamEventDto> events = new ArrayList<>();
        service.handleMessage(userId,
                new SendChatMessageRequest("Chào bạn", null, null),
                Locale.of("vi"), events::add);
        assertThat(events).extracting(ChatStreamEventDto::type)
                .containsExactly("TEXT_DELTA", "DONE");
    }

    @Test
    void handleMessage_streamFailure_persistsPartialAndEmitsError() {
        when(chatModel.generate(any(ChatRequest.class))).thenReturn(new ChatResponse(null, List.of(
                new FunctionCall("search_places", Map.of("query", "cơm", "limit", 5)))));
        when(chatTools.execute(any(FunctionCall.class), any()))
                .thenReturn(new ChatTools.ToolOutcome(java.util.Map.of(), List.of()));
        doAnswer(inv -> {
            throw new ChatModelException("Gemini loi 500");
        }).when(chatModel).stream(any(ChatRequest.class), any());

        List<ChatStreamEventDto> events = new ArrayList<>();
        service.handleMessage(userId,
                new SendChatMessageRequest("Tìm cơm", null, "vi"),
                Locale.of("vi"), events::add);

        // Tin trợ lý vẫn được lưu (dù rỗng) và có ERROR, không ném ra ngoài.
        ArgumentCaptor<ChatMessage> saved = ArgumentCaptor.forClass(ChatMessage.class);
        verify(messages, org.mockito.Mockito.times(2)).save(saved.capture());
        assertThat(saved.getAllValues().get(1).getRole()).isEqualTo(ChatRole.ASSISTANT);
        assertThat(events).extracting(ChatStreamEventDto::type).containsExactly("ERROR");
        assertThat(events.get(0).error()).isNotNull();
    }

    @Test
    void getCurrentSession_returnsHistory() {
        ChatMessage m = new ChatMessage(session, ChatRole.USER, "hello", List.of());
        m.setId(UUID.randomUUID());
        when(messages.findBySessionOrderByCreatedAtAsc(session)).thenReturn(List.of(m));
        var dto = service.getCurrentSession(userId);
        assertThat(dto.id()).isEqualTo(session.getId());
        assertThat(dto.messages()).hasSize(1);
        assertThat(dto.messages().get(0).content()).isEqualTo("hello");
    }

    @Test
    void clearSession_deletesSession() {
        service.clearSession(userId);
        verify(sessions).delete(session);
    }

    @Test
    void functionCallPart_isNotMistakenForUserMessageInHistory() {
        // Hàm buildHistory: tin ASSISTANT chỉ có tool call mà không có text
        // được đổi thành "[đang tra cứu]" — kiểm tra qua request gửi cho model.
        when(chatModel.generate(any(ChatRequest.class))).thenAnswer(inv -> {
            ChatRequest req = inv.getArgument(0);
            if (req.history().size() == 1) {
                return new ChatResponse(null, List.of(
                        new FunctionCall("get_place_details", Map.of("placeId", UUID.randomUUID().toString()))));
            }
            // Vòng 2: history = tin user + lượt gọi tool của assistant + kết quả
            // tool trả về (dạng tin assistant). Không có bản sao tin user hiện tại.
            assertThat(req.history()).hasSize(3);
            assertThat(req.history().get(1).parts()).hasSize(1);
            assertThat(req.history().get(1).parts().get(0)).isInstanceOf(FunctionCallPart.class);
            assertThat(req.history().get(1).parts().get(0)).isNotInstanceOf(TextPart.class);
            assertThat(req.history().get(2).parts().get(0)).isInstanceOf(FunctionResponsePart.class);
            return new ChatResponse("Xong.", List.of());
        });
        when(chatTools.execute(any(FunctionCall.class), any()))
                .thenReturn(new ChatTools.ToolOutcome(java.util.Map.of(), List.of()));
        List<ChatStreamEventDto> events = new ArrayList<>();
        service.handleMessage(userId,
                new SendChatMessageRequest("Chi tiết quán này", null, "vi"),
                Locale.of("vi"), events::add);
        assertThat(events).extracting(ChatStreamEventDto::type)
                .containsExactly("TEXT_DELTA", "DONE");
    }
}
