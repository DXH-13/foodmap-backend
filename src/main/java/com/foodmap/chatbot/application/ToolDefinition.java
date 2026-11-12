package com.foodmap.chatbot.application;

import java.util.Map;

/**
 * Khai báo một tool nội bộ cho model: tên, mô tả (model đọc để quyết định khi
 * nào gọi), và JSON Schema của tham số.
 *
 * <p>{@code parameters} là object JSON Schema đầy đủ, ví dụ
 * {@code {"type": "object", "properties": {...}, "required": [...]}}.
 */
public record ToolDefinition(
        String name,
        String description,
        Map<String, Object> parameters
) {
}
