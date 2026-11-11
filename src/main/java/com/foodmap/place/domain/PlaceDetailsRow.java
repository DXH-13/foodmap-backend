package com.foodmap.place.domain;

import java.util.UUID;

/** Kết quả thô của truy vấn lấy thông tin tối thiểu của một địa điểm. */
public interface PlaceDetailsRow {

    UUID getId();

    /** Đã áp dụng fallback locale ở tầng SQL — không bao giờ null. */
    String getName();

    double getLatitude();

    double getLongitude();

    String getStatus();
}
