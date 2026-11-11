/**
 * Giao diện công khai của module {@code place}.
 *
 * <p>Modulith 2.1.2 không tự mở package {@code api/} — thiếu annotation này thì
 * module khác (review, favorite, visit) dùng {@link com.foodmap.place.api.PlaceApi}
 * sẽ bị báo "depends on non-exposed type" khi chạy {@code ModulithTest}.
 */
@org.springframework.modulith.NamedInterface("api")
package com.foodmap.place.api;
