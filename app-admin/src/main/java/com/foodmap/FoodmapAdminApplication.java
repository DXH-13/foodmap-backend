package com.foodmap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * API quản trị — phục vụ trang admin Next.js. Cổng 8081.
 *
 * <p>Class nằm ở package {@code com.foodmap} là chủ ý: component scan mặc định lấy
 * package của class này làm gốc, nhờ vậy quét được cả bean của module {@code core}
 * mà không cần {@code @EntityScan} hay {@code @EnableJpaRepositories}.
 *
 * <p>Đây là app <b>KHÔNG chạy Flyway</b> — lược đồ do app-public quản lý.
 * Xem {@code application.yml}.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class FoodmapAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(FoodmapAdminApplication.class, args);
    }
}
