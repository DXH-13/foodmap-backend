package com.foodmap.common.exception;

import com.foodmap.common.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.List;
import java.util.UUID;

/**
 * Chuyển mọi exception thành {@link ApiError} — đúng schema trong openapi.yaml.
 *
 * <p>Ba nguyên tắc:
 * <ul>
 *   <li>{@code code} không dịch; {@code message} dịch theo {@code Accept-Language}.</li>
 *   <li>Không bao giờ để stack trace hay thông tin nội bộ lọt ra response.</li>
 *   <li>Mọi phản hồi đều có {@code traceId} để đối chiếu với log.</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final MessageSource messages;

    public GlobalExceptionHandler(MessageSource messages) {
        this.messages = messages;
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> handleBusiness(BusinessException ex, HttpServletRequest request) {
        String traceId = newTraceId();
        log.info("Loi nghiep vu [{}] {} {} -> {}", traceId, request.getMethod(), request.getRequestURI(), ex.getCode());
        return ResponseEntity
                .status(ex.getStatus())
                .body(ApiError.of(ex.getCode(), translate(ex.getMessageKey(), ex.getMessageArgs()), traceId));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        List<ApiError.FieldErrorDetail> details = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toFieldDetail)
                .toList();
        return ResponseEntity
                .badRequest()
                .body(new ApiError(
                        "VALIDATION_FAILED",
                        translate("common.error.validation"),
                        details,
                        newTraceId()));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthentication(AuthenticationException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiError.of("UNAUTHORIZED", translate("common.error.unauthorized"), newTraceId()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiError.of("FORBIDDEN", translate("common.error.forbidden"), newTraceId()));
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiError> handleNoHandler(NoHandlerFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiError.of("NOT_FOUND", translate("common.error.not_found"), newTraceId()));
    }

    /**
     * Lưới an toàn cuối cùng. Log đầy đủ ở phía server, nhưng chỉ trả về thông báo
     * chung chung kèm {@code traceId} cho client.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
        String traceId = newTraceId();
        log.error("Loi khong luong truoc [{}] {} {}", traceId, request.getMethod(), request.getRequestURI(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiError.of("INTERNAL_ERROR", translate("common.error.internal"), traceId));
    }

    private ApiError.FieldErrorDetail toFieldDetail(FieldError error) {
        return new ApiError.FieldErrorDetail(
                error.getField(),
                error.getCode() == null ? "INVALID" : error.getCode(),
                error.getDefaultMessage() == null ? translate("common.error.validation") : error.getDefaultMessage());
    }

    private String translate(String key, Object... args) {
        return messages.getMessage(key, args, key, LocaleContextHolder.getLocale());
    }

    private static String newTraceId() {
        return UUID.randomUUID().toString();
    }
}
