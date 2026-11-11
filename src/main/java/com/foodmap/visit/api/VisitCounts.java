package com.foodmap.visit.api;

/**
 * Số liệu ghé thăm của một địa điểm, do module {@code visit} tính từ dữ liệu
 * của nó để module {@code place} áp vào các cột dẫn xuất.
 */
public record VisitCounts(long visitCount, long distinctVisitorCount) {
}
