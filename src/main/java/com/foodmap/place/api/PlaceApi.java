package com.foodmap.place.api;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
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
     * Tìm địa điểm quanh một toạ độ, sắp xếp theo khoảng cách tăng dần.
     * Chỉ trả địa điểm đang hiển thị công khai (PUBLISHED, TEMPORARILY_CLOSED).
     *
     * <p>Thêm cho tool {@code search_places} của chatbot ở Bước 5 — chatbot gọi
     * qua đây thay vì đọc thẳng bảng {@code places}.
     *
     * @param radiusMeters null thì dùng bán kính mặc định của cấu hình.
     * @param limit số kết quả tối đa, ép về khoảng 1..10.
     * @throws com.foodmap.common.BadRequestException mã {@code RADIUS_OUT_OF_RANGE}
     *         hoặc {@code INVALID_COORDINATES} khi tham số ngoài khoảng cho phép.
     */
    List<PlaceSummary> searchNearby(double latitude, double longitude, Integer radiusMeters, int limit);

    /**
     * Tìm địa điểm theo từ khoá trên tên và mô tả, không phân biệt hoa thường và
     * không phân biệt dấu (dùng {@code f_unaccent}). Chỉ trả địa điểm đang hiển
     * thị công khai, sắp xếp theo điểm trung bình rồi số đánh giá.
     *
     * <p>Thêm cho tool {@code search_places} của chatbot ở Bước 5, cho câu hỏi
     * không kèm toạ độ (ví dụ "quán phở ngon ở Quận 1").
     *
     * @param limit số kết quả tối đa, ép về khoảng 1..10.
     */
    List<PlaceSummary> searchByKeyword(String keyword, int limit);

    /**
     * Người đã tạo địa điểm (cột {@code created_by}), nếu có.
     *
     * <p>Rỗng khi địa điểm không tồn tại, đã xoá mềm, hoặc được tạo từ dữ liệu
     * gốc mà không gắn người tạo (seed). Module {@code notification} ở Bước 5
     * dùng để biết báo cho ai khi quán có đánh giá mới — chỉ đọc, không sửa.
     */
    Optional<UUID> findCreatorById(UUID placeId);

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
