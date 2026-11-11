package com.foodmap.review.api;

import java.util.UUID;

/**
 * Đánh giá mới vừa được tạo (trạng thái {@code PENDING}, chưa ảnh hưởng tới
 * điểm trung bình cho tới khi được duyệt).
 *
 * <p>Người nghe hiện tại: {@code ReviewPlaceAggregateListener} (cùng module
 * review — xem ghi chú ở đó vì sao listener không đặt ở module place) đồng bộ
 * số liệu sang {@code place}; module {@code notification} ở Bước 5 sẽ nghe thêm
 * để báo chủ quán.
 */
public record ReviewCreated(UUID placeId, UUID reviewId) {
}
