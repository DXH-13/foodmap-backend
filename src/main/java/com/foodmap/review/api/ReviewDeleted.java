package com.foodmap.review.api;

import java.util.UUID;

/** Đánh giá vừa bị tác giả xoá (xoá mềm). */
public record ReviewDeleted(UUID placeId, UUID reviewId) {
}
