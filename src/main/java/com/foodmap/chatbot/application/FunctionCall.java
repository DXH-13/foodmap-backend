package com.foodmap.chatbot.application;

import java.util.Map;

/** Một lời gọi tool do model yêu cầu: tên tool + tham số dạng JSON. */
public record FunctionCall(
        String name,
        Map<String, Object> args
) {
}
