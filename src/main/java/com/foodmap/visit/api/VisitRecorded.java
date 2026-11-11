package com.foodmap.visit.api;

import java.util.UUID;

/**
 * Một lượt đến vừa được ghi nhận.
 *
 * <p>Người nghe hiện tại: {@code VisitPlaceAggregateListener} (cùng module
 * visit — cùng lý do vòng tròn như {@code ReviewPlaceAggregateListener} ở
 * module review) đồng bộ số liệu sang {@code place}; module {@code user} ở
 * bước sau có thể nghe thêm để cập nhật gợi ý, module {@code review} để nhắc
 * đánh giá.
 */
public record VisitRecorded(UUID placeId, UUID visitId) {
}
