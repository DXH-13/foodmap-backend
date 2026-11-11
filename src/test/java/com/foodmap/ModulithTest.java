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
 * <p>Hiện tại các package {@code common} và {@code config} vẫn được Modulith nhận
 * diện mặc định thành module (chúng là package con trực tiếp của {@code com.foodmap})
 * — ranh giới của chúng được giữ bằng cách chỉ cho module khác dùng type ở package
 * gốc (ví dụ exception ở {@code com.foodmap.common}) hoặc qua {@code api/}.
 * Module {@code place} (Bước 2) và {@code identity} / {@code user} (Bước 3) đã
 * được gói theo khuôn.
 */
class ModulithTest {

    @Test
    void verifyModuleBoundaries() {
        ApplicationModules.of(FoodmapBackendApplication.class).verify();
    }
}
