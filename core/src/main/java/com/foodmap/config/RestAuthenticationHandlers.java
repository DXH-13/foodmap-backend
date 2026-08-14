package com.foodmap.config;

import com.foodmap.common.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
// Spring Boot 4 dùng Jackson 3 — package `tools.jackson`, không phải `com.fasterxml.jackson`.
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Locale;
import java.util.UUID;

/**
 * Trả lỗi bảo mật đúng hình dạng {@link ApiError}, giống mọi lỗi khác của API.
 *
 * <p>Không có cấu hình này, Spring Security trả HTML mặc định — và tệ hơn, trả
 * <b>403 cho request chưa đăng nhập</b> thay vì 401. Client không phân biệt được
 * "cần đăng nhập" (nên mở màn hình đăng nhập) với "không đủ quyền" (nên báo lỗi).
 */
@Configuration
public class RestAuthenticationHandlers {

    private static final Locale VIETNAMESE = Locale.forLanguageTag("vi");

    private final MessageSource messages;
    private final ObjectMapper objectMapper;

    public RestAuthenticationHandlers(MessageSource messages, ObjectMapper objectMapper) {
        this.messages = messages;
        this.objectMapper = objectMapper;
    }

    /** 401 — chưa đăng nhập hoặc token không hợp lệ. */
    @Bean
    public AuthenticationEntryPoint restAuthenticationEntryPoint() {
        return (request, response, ex) -> writeError(
                request, response, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "common.error.unauthorized");
    }

    /** 403 — đã đăng nhập nhưng không đủ quyền. */
    @Bean
    public AccessDeniedHandler restAccessDeniedHandler() {
        return (request, response, ex) -> writeError(
                request, response, HttpStatus.FORBIDDEN, "FORBIDDEN", "common.error.forbidden");
    }

    private void writeError(HttpServletRequest request,
                            HttpServletResponse response,
                            HttpStatus status,
                            String code,
                            String messageKey) throws IOException {

        String message = messages.getMessage(messageKey, null, messageKey, resolveLocale(request));

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(
                response.getOutputStream(),
                ApiError.of(code, message, UUID.randomUUID().toString()));
    }

    /**
     * Tự đọc {@code Accept-Language} thay vì dùng {@code LocaleContextHolder}.
     *
     * <p>Lý do: filter bảo mật chạy <b>trước</b> DispatcherServlet, nên
     * {@code LocaleContextHolder} lúc này vẫn đang giữ locale mặc định của JVM —
     * tức là thông báo sẽ theo ngôn ngữ của máy chủ chứ không theo người dùng.
     * Một lỗi âm thầm: trên máy dev tiếng Anh thì trông vẫn "đúng".
     */
    private Locale resolveLocale(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.ACCEPT_LANGUAGE);
        if (header != null && header.toLowerCase(Locale.ROOT).startsWith("en")) {
            return Locale.ENGLISH;
        }
        return VIETNAMESE;
    }
}
