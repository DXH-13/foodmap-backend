package com.foodmap.place;

import java.util.Set;

/**
 * Trạng thái địa điểm. Khớp constraint {@code places_status_check} trong V1__init_schema.sql.
 */
public enum PlaceStatus {

    /** Đề xuất mới hoặc bản nháp của admin — chưa hiển thị công khai. */
    DRAFT,

    /** Đang hoạt động, hiển thị công khai. */
    PUBLISHED,

    /** Tạm đóng cửa — vẫn hiển thị công khai kèm nhãn. */
    TEMPORARILY_CLOSED,

    /** Đã đóng cửa vĩnh viễn — không hiển thị ở API công khai. */
    PERMANENTLY_CLOSED;

    /** Các trạng thái được phép xuất hiện ở API công khai (FR-PLACE-09). */
    public static final Set<PlaceStatus> PUBLICLY_VISIBLE =
            Set.of(PUBLISHED, TEMPORARILY_CLOSED);
}
