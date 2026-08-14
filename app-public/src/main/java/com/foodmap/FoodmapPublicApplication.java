package com.foodmap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * API công khai — phục vụ ứng dụng di động. Cổng 8080.
 *
 * <p>Class nằm ở package {@code com.foodmap} là chủ ý: component scan mặc định lấy
 * package của class này làm gốc, nhờ vậy quét được cả bean của module {@code core}
 * mà không cần {@code @EntityScan} hay {@code @EnableJpaRepositories}.
 *
 * <p>Đây là app <b>chạy Flyway</b> — xem {@code application.yml}.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class FoodmapPublicApplication {

    public static void main(String[] args) {
        SpringApplication.run(FoodmapPublicApplication.class, args);
    }
}
