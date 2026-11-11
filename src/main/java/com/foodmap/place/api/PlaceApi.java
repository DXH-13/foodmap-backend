package com.foodmap.place.api;

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
}
