package com.foodmap.visit.api;

import java.util.UUID;

/**
 * Giao diện công khai của module {@code visit} cho module khác gọi qua {@code api/}.
 *
 * <p>Hiện chỉ có một phương thức: module {@code place} cần số liệu này để cập
 * nhật các cột dẫn xuất ({@code visit_count}, {@code distinct_visitor_count})
 * mà không được đọc thẳng bảng {@code visits}.
 */
public interface VisitApi {

    /** Tổng số lượt đến và số người khác nhau đã đến của một địa điểm. */
    VisitCounts countsForPlace(UUID placeId);
}
