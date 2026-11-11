package com.foodmap;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/**
 * Kiểm tra ranh giới module (Bước 2, kiến trúc BE thống nhất).
 *
 * <p>Chạy trong CI qua {@code ./gradlew build} — vi phạm ranh giới (module này
 * import thẳng vào phần nội bộ của module khác, hoặc phụ thuộc vòng tròn) sẽ
 * làm đỏ build ngay khi nộp code, thay vì chờ tới khi hệ thống khó sửa.
 *
 * <p>Hiện tại chỉ module {@code place} được gói lại theo khuôn; các package
 * {@code auth}, {@code common}, {@code config} vẫn bị Modulith nhận diện
 * mặc định thành module (chúng là package con trực tiếp của {@code com.foodmap})
 * và ranh giới của chúng hiện đang vi phạm — xem ghi chú trong báo cáo Bước 2.
 * Chúng được siết dần ở các bước sau khi từng module được tách ra
 * (xem tài liệu kiến trúc, mục "Lộ trình").
 */
class ModulithTest {

    @Test
    void verifyModuleBoundaries() {
        ApplicationModules.of(FoodmapBackendApplication.class).verify();
    }
}
