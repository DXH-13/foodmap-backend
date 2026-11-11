package com.foodmap.review.api;

/** Hành động kiểm duyệt một đánh giá. Khớp enum {@code action} của {@code ModerateReviewRequest} trong hợp đồng. */
public enum ModerateAction {
    APPROVE,
    REJECT,
    HIDE,
    UNHIDE
}
