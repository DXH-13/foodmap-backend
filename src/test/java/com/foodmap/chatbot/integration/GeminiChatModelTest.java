package com.foodmap.chatbot.integration;

import tools.jackson.databind.ObjectMapper;
import com.foodmap.chatbot.application.ChatModelException;
import com.foodmap.chatbot.application.ChatRequest;
import com.foodmap.chatbot.application.ChatResponse;

import com.foodmap.chatbot.ChatProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestClient;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Kiểm thử adapter Gemini với một {@code ClientHttpRequestFactory} giả lập —
 * KHÔNG gọi API Gemini thật, KHÔNG dùng key thật, và KHÔNG mở socket nào.
 *
 * <p>Lý do không dùng JDK HttpServer: sandbox reset mọi kết nối loopback do
 * chính Java mở (curl/Python tới cùng server vẫn chạy), nên server giả +
 * RestClient thật luôn nhận status -1. Factory giả lập vẫn kiểm tra được toàn
 * bộ adapter: URI, retry 500→200, không retry với 400, và parse SSE.
 */
class GeminiChatModelTest {

    /** Một response giả: status + body + header, trả theo thứ tự gọi. */
    record StubResponse(int status, String body, Map<String, String> headers) {
        StubResponse(int status, String body) {
            this(status, body, Map.of());
        }
    }

    static class StubRequestFactory implements ClientHttpRequestFactory {
        final Queue<StubResponse> responses = new LinkedList<>();
        final List<URI> requestedUris = new ArrayList<>();
        final AtomicInteger calls = new AtomicInteger();

        StubRequestFactory(StubResponse... responses) {
            this.responses.addAll(List.of(responses));
        }

        @Override
        public ClientHttpRequest createRequest(URI uri, HttpMethod method) {
            requestedUris.add(uri);
            calls.incrementAndGet();
            return new ClientHttpRequest() {
                final ByteArrayOutputStream body = new ByteArrayOutputStream();

                @Override
                public HttpMethod getMethod() {
                    return method;
                }

                @Override
                public URI getURI() {
                    return uri;
                }

                @Override
                public HttpHeaders getHeaders() {
                    return new HttpHeaders();
                }

                @Override
                public Map<String, Object> getAttributes() {
                    // RestClient ghi attribute vào map này — phải mutable.
                    return new java.util.HashMap<>();
                }

                @Override
                public OutputStream getBody() {
                    return body;
                }

                @Override
                public ClientHttpResponse execute() {
                    StubResponse stub = responses.poll();
                    if (stub == null) {
                        stub = new StubResponse(500, "{\"error\":{\"message\":\"het response gia\"}}");
                    }
                    return new StubClientHttpResponse(stub);
                }
            };
        }
    }

    static class StubClientHttpResponse implements ClientHttpResponse {
        final StubResponse stub;

        StubClientHttpResponse(StubResponse stub) {
            this.stub = stub;
        }

        @Override
        public HttpStatusCode getStatusCode() {
            return HttpStatusCode.valueOf(stub.status());
        }

        @Override
        public String getStatusText() {
            org.springframework.http.HttpStatus status =
                    org.springframework.http.HttpStatus.resolve(stub.status());
            return status != null ? status.getReasonPhrase() : "";
        }

        @Override
        public HttpHeaders getHeaders() {
            HttpHeaders headers = new HttpHeaders();
            stub.headers().forEach(headers::set);
            return headers;
        }

        @Override
        public InputStream getBody() {
            return new ByteArrayInputStream(stub.body().getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public void close() {
            // Không có kết nối thật để đóng.
        }
    }

    GeminiChatModel model(StubRequestFactory factory) {
        ChatProperties properties = new ChatProperties("fake-key", "gemini-2.0-flash",
                "https://generativelanguage.googleapis.com/v1beta", 30);
        RestClient restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(factory)
                .build();
        return new GeminiChatModel(restClient, new ObjectMapper(), properties);
    }

    ChatRequest emptyRequest() {
        return new ChatRequest("sys", List.of(), List.of());
    }

    @Test
    void generate_parsesTextAndFunctionCall() {
        StubRequestFactory factory = new StubRequestFactory(new StubResponse(200, """
                {"candidates":[{"content":{"parts":[
                  {"text":"Để tôi tìm giúp bạn."},
                  {"functionCall":{"name":"search_places","args":{"query":"phở","limit":5}}}
                ],"finishReason":"STOP"}}]}
                """));
        ChatResponse response = model(factory).generate(emptyRequest());
        assertThat(response.text()).isEqualTo("Để tôi tìm giúp bạn.");
        assertThat(response.functionCalls()).hasSize(1);
        assertThat(response.functionCalls().get(0).name()).isEqualTo("search_places");
        assertThat(response.functionCalls().get(0).args()).containsEntry("query", "phở");
        // Key gửi qua query param, đúng kiểu ClassTikTok dùng fetch.
        assertThat(factory.requestedUris.get(0).toString())
                .contains(":generateContent")
                .contains("key=fake-key");
    }

    @Test
    void generate_retriesOnceOn500_thenSucceeds() {
        StubRequestFactory factory = new StubRequestFactory(
                new StubResponse(500, "{\"error\":{\"message\":\"overloaded\"}}"),
                new StubResponse(200, """
                        {"candidates":[{"content":{"parts":[{"text":"ok"}]},"finishReason":"STOP"}]}
                        """));
        ChatResponse response = model(factory).generate(emptyRequest());
        assertThat(response.text()).isEqualTo("ok");
        assertThat(factory.calls.get()).isEqualTo(2);
    }

    @Test
    void generate_throwsChatModelExceptionOn400_withoutRetry() {
        StubRequestFactory factory = new StubRequestFactory(
                new StubResponse(400, "{\"error\":{\"message\":\"bad key\"}}"));
        assertThatThrownBy(() -> model(factory).generate(emptyRequest()))
                .isInstanceOf(ChatModelException.class)
                .hasMessageContaining("HTTP 400");
        assertThat(factory.calls.get()).isEqualTo(1);
    }

    @Test
    void isConfigured_falseWhenKeyBlank() {
        ChatProperties noKey = new ChatProperties("", "gemini-2.0-flash",
                "https://generativelanguage.googleapis.com/v1beta", 30);
        GeminiChatModel unconfigured = new GeminiChatModel(
                RestClient.builder().build(), new ObjectMapper(), noKey);
        assertThat(unconfigured.isConfigured()).isFalse();
        ChatProperties withKey = new ChatProperties("fake-key", "gemini-2.0-flash",
                "https://generativelanguage.googleapis.com/v1beta", 30);
        GeminiChatModel configured = new GeminiChatModel(
                RestClient.builder().build(), new ObjectMapper(), withKey);
        assertThat(configured.isConfigured()).isTrue();
    }

    @Test
    void stream_parsesSseDataLines_inOrder() {
        String sseBody = "data: {\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"Xin \"}]}}]}\n"
                + "data: {\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"chào\"}]}}]}\n"
                + "data: {\"candidates\":[{\"content\":{\"parts\":[{\"text\":\" bạn\"}]}}]}\n";
        StubRequestFactory factory = new StubRequestFactory(new StubResponse(200, sseBody));
        List<String> chunks = new ArrayList<>();
        model(factory).stream(emptyRequest(), chunks::add);
        assertThat(chunks).containsExactly("Xin ", "chào", " bạn");
        assertThat(factory.requestedUris.get(0).toString())
                .contains(":streamGenerateContent")
                .contains("alt=sse");
    }

    @Test
    void stream_throwsChatModelExceptionOn500_withoutSuccessfulRetry() {
        // 500 có retry 1 lần; cả 2 lần đều 500 → ChatModelException.
        StubRequestFactory factory = new StubRequestFactory(
                new StubResponse(500, "{\"error\":{\"message\":\"overloaded\"}}"),
                new StubResponse(500, "{\"error\":{\"message\":\"overloaded\"}}"));
        assertThatThrownBy(() -> model(factory).stream(emptyRequest(), s -> {
        })).isInstanceOf(ChatModelException.class);
        assertThat(factory.calls.get()).isEqualTo(2);
    }
}
