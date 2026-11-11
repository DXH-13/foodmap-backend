package com.foodmap.review.api;

import java.util.UUID;

/**
 * Đánh giá đã có vừa được tác giả gửi lại (cập nhật nội dung / số sao).
 * Gửi lại luôn đưa đánh giá về {@code PENDING} để kiểm duyệt lại, nên điểm
 * trung bình có thể thay đổi ngay cả khi đánh giá từng được duyệt.
 */
public record ReviewUpdated(UUID placeId, UUID reviewId) {
}
