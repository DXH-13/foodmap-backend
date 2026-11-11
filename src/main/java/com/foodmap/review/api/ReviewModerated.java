package com.foodmap.review.api;

import java.util.UUID;

/**
 * Đánh giá vừa được kiểm duyệt (duyệt / từ chối / ẩn / bỏ ẩn).
 * Quyết định kiểm duyệt có thể làm thay đổi điểm trung bình (ví dụ duyệt một
 * đánh giá đang chờ, hoặc ẩn một đánh giá đã duyệt).
 */
public record ReviewModerated(UUID placeId, UUID reviewId, ReviewStatus status) {
}
