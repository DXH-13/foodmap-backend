package com.foodmap.place;

import java.time.Instant;

/** Kết quả thô của {@link PlaceRepository#findPublicDetail}: cột rút gọn + cột riêng của trang chi tiết. */
public interface PlaceDetailProjection extends PlaceSummaryProjection {

    /** Đã fallback về bản tiếng Việt; {@code null} khi cả hai bản đều không có mô tả. */
    String getDescription();

    String getPhone();

    long getDistinctVisitorCount();

    Instant getCreatedAt();
}
