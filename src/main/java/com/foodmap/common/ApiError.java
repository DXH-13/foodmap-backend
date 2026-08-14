package com.foodmap.common;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Hình dạng lỗi thống nhất cho toàn bộ API. Khớp với schema {@code ApiError}
 * trong docs/03-api/openapi.yaml.
 *
 * @param code    mã lỗi ổn định, SCREAMING_SNAKE_CASE. <b>Không dịch</b> —
 *                client so sánh bằng mã này, không bằng {@code message}.
 * @param message thông báo cho người dùng, <b>đã dịch</b> theo {@code Accept-Language}
 * @param details chi tiết theo từng trường, chỉ có ở lỗi validation
 * @param traceId mã truy vết để đối chiếu với log phía server
 */
@Schema(description = "Hình dạng lỗi thống nhất cho toàn bộ API")
public record ApiError(
        @Schema(example = "PLACE_NOT_FOUND") String code,
        @Schema(example = "Không tìm thấy địa điểm.") String message,
        List<FieldErrorDetail> details,
        String traceId
) {

    public static ApiError of(String code, String message, String traceId) {
        return new ApiError(code, message, null, traceId);
    }

    /**
     * @param field   tên trường bị lỗi
     * @param code    mã lỗi validation, ví dụ {@code NotBlank}
     * @param message mô tả đã dịch
     */
    public record FieldErrorDetail(String field, String code, String message) {}
}
