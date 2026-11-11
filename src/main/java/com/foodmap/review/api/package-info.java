/**
 * Giao diện công khai của module {@code review}: hợp đồng cho module khác gọi
 * ({@link com.foodmap.review.api.ReviewApi}) và các sự kiện module này phát.
 *
 * <p>Modulith 2.1.2 không tự mở package {@code api/} — thiếu annotation này thì
 * module khác dùng {@code ReviewApi} hay nghe sự kiện sẽ bị báo
 * "depends on non-exposed type" khi chạy {@code ModulithTest}.
 */
@org.springframework.modulith.NamedInterface("api")
package com.foodmap.review.api;
