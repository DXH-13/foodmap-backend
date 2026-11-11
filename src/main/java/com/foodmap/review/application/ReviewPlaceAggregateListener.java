package com.foodmap.review.application;

import com.foodmap.place.api.PlaceApi;
import com.foodmap.review.api.ReviewApi;
import com.foodmap.review.api.ReviewCreated;
import com.foodmap.review.api.ReviewDeleted;
import com.foodmap.review.api.ReviewModerated;
import com.foodmap.review.api.ReviewStats;
import com.foodmap.review.api.ReviewUpdated;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Đồng bộ điểm trung bình của địa điểm sau mỗi thay đổi đánh giá.
 *
 * <p><b>Vì sao listener này nằm ở module {@code review} chứ không phải ở
 * {@code place} như sơ đồ kiến trúc gợi ý:</b> {@code review} bắt buộc phải gọi
 * {@code PlaceApi} (kiểm tra địa điểm tồn tại, lấy tên, tính khoảng cách cho
 * check-in), còn listener ở {@code place} thì phải import kiểu sự kiện của
 * {@code review}. Hai chiều đó tạo thành phụ thuộc vòng tròn và
 * {@code ModulithTest.verify()} báo lỗi — đã kiểm chứng bằng một mô hình thu
 * nhỏ (hai module giả lập, một bên gọi API của bên kia, bên kia nghe sự kiện
 * của bên này) trước khi viết code thật. Đặt listener ở đây thì chỉ còn một
 * chiều {@code review → place}, không có vòng tròn.
 *
 * <p>Luồng sự kiện vẫn giữ nguyên ý đồ của kiến trúc: {@code ReviewService}
 * phát sự kiện trong cùng transaction với thay đổi nghiệp vụ, outbox
 * (bảng {@code event_publication}) ghi lại, listener này chạy sau commit trong
 * transaction riêng của nó và gọi {@code PlaceApi} để áp số liệu. Nếu listener
 * lỗi, bản ghi outbox ở lại trạng thái chưa hoàn thành và được thử lại — số
 * liệu địa điểm chỉ chậm cập nhật chứ không mất.
 */
@Component
public class ReviewPlaceAggregateListener {

    private final ReviewApi reviewApi;
    private final PlaceApi placeApi;

    public ReviewPlaceAggregateListener(ReviewApi reviewApi, PlaceApi placeApi) {
        this.reviewApi = reviewApi;
        this.placeApi = placeApi;
    }

    @ApplicationModuleListener
    public void on(ReviewCreated event) {
        sync(event.placeId());
    }

    @ApplicationModuleListener
    public void on(ReviewUpdated event) {
        sync(event.placeId());
    }

    @ApplicationModuleListener
    public void on(ReviewDeleted event) {
        sync(event.placeId());
    }

    @ApplicationModuleListener
    public void on(ReviewModerated event) {
        sync(event.placeId());
    }

    /**
     * Tính lại từ các đánh giá <b>đã duyệt</b> rồi áp vào địa điểm. Tính lại toàn
     * bộ thay vì cộng/trừ dần — tránh sai số tích luỹ khi có sửa/xoá/ẩn.
     */
    private void sync(UUID placeId) {
        ReviewStats stats = reviewApi.statsForPlace(placeId);
        placeApi.updateRatingStats(placeId, stats.averageRating(), stats.reviewCount());
    }
}
