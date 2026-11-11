package com.foodmap.review.api;

import java.math.BigDecimal;

/**
 * Số liệu đánh giá đã duyệt của một địa điểm, do module {@code review} tính từ
 * dữ liệu của nó để module {@code place} áp vào các cột dẫn xuất.
 *
 * @param averageRating {@code null} khi chưa có đánh giá đã duyệt nào — không phải 0.
 */
public record ReviewStats(BigDecimal averageRating, int reviewCount) {
}
