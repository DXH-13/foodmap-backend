package com.foodmap.place.api;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Giao diện công khai của module {@code place} cho module khác gọi qua {@code api/}.
 *
 * <p>Module khác chỉ được dùng những gì ở đây — không import thẳng vào
 * {@code domain/}, {@code application/}, {@code web/} hay {@code infrastructure/}
 * của {@code place}. Đây là mối nối cho module {@code review} ở Bước 3: trước khi
 * gắn một đánh giá vào địa điểm, {@code review} gọi {@code existsById} thay vì
 * đọc thẳng bảng {@code places}.
 */
public interface PlaceApi {

    /**
     * Địa điểm tồn tại và chưa bị xoá mềm.
     *
     * <p>Không kiểm tra trạng thái hiển thị công khai: địa điểm nháp vẫn có thể
     * nhận dữ liệu nội bộ, việc có hiển thị ra API công khai hay không là quyết
     * định riêng của {@code place}.
     */
    boolean existsById(UUID placeId);

    /**
     * Thông tin tối thiểu của một địa điểm (tên, toạ độ, trạng thái).
     *
     * @throws com.foodmap.common.NotFoundException mã {@code PLACE_NOT_FOUND} khi không có.
     */
    PlaceDetails getPlace(UUID placeId);

    /**
     * Khoảng cách (mét) từ một toạ độ tới địa điểm, tính bằng PostGIS trên cột
     * geography — chính xác hơn công thức Haversine viết tay.
     *
     * @throws com.foodmap.common.NotFoundException mã {@code PLACE_NOT_FOUND} khi không có.
     */
    double distanceMetersTo(UUID placeId, double latitude, double longitude);

    /**
     * Địa điểm rút gọn cho một danh sách id (ví dụ danh sách yêu thích).
     * Chỉ trả những địa điểm còn tồn tại, thứ tự không đảm bảo — module
     * gọi tự sắp xếp theo nhu cầu của nó. Danh sách rỗng trả về rỗng, không lỗi.
     *
     * @param locale ngôn ngữ tên hiển thị, thiếu thì lùi về tiếng Việt.
     */
    List<PlaceSummary> getSummaries(List<UUID> placeIds, String locale);

    /**
     * Cập nhật điểm trung bình và số đánh giá đã duyệt của một địa điểm.
     *
     * <p>Đây là điểm duy nhất module khác được phép thay đổi dữ liệu dẫn xuất của
     * {@code place}: module gọi tự tính số liệu từ dữ liệu của nó rồi đưa sang
     * đây áp dụng — {@code place} không đọc bảng của module khác, module khác
     * không ghi thẳng bảng {@code places}. Dùng từ listener sự kiện sau commit
     * (xem {@code ReviewPlaceAggregateListener} ở module review).
     *
     * @param averageRating {@code null} khi chưa có đánh giá đã duyệt nào — không phải 0.
     * @throws com.foodmap.common.NotFoundException mã {@code PLACE_NOT_FOUND} khi không có.
     */
    void updateRatingStats(UUID placeId, BigDecimal averageRating, int reviewCount);

    /**
     * Cập nhật tổng số lượt đến và số người khác nhau đã đến của một địa điểm.
     * Cùng nguyên tắc như {@link #updateRatingStats}: module gọi tính số liệu
     * từ dữ liệu của nó, {@code place} chỉ áp dụng.
     *
     * @throws com.foodmap.common.NotFoundException mã {@code PLACE_NOT_FOUND} khi không có.
     */
    void updateVisitStats(UUID placeId, long visitCount, long distinctVisitorCount);
}
