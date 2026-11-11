package com.foodmap.review.api;

/**
 * Trạng thái kiểm duyệt của một đánh giá. Khớp enum {@code ReviewStatus} trong
 * hợp đồng và CHECK trên bảng {@code reviews}.
 *
 * <p>Đặt ở {@code api/} vì nó xuất hiện trong JSON trả ra, trong request kiểm
 * duyệt và trong sự kiện — tức là một phần của hợp đồng công khai, không phải
 * chi tiết nội bộ của domain (xem tiền lệ {@code UserRole} ở Bước 3).
 */
public enum ReviewStatus {
    PENDING,
    APPROVED,
    REJECTED,
    HIDDEN
}
