package com.foodmap.review.web.dto;

import com.foodmap.review.api.ModerateAction;
import jakarta.validation.constraints.NotNull;

/**
 * Quyết định kiểm duyệt. Khớp schema {@code ModerateReviewRequest} trong hợp đồng.
 *
 * <p>{@code moderationNote} bắt buộc khi {@code action = REJECT} — thiếu thì
 * service trả 400 {@code MODERATION_NOTE_REQUIRED}.
 */
public record ModerateReviewRequest(
        @NotNull ModerateAction action,
        String moderationNote
) {
}
