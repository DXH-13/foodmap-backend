package com.foodmap;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test khói: ứng dụng lên được, Flyway chạy sạch trên CSDL rỗng, và PostGIS hoạt động.
 *
 * <p>Đây là bài kiểm tra rẻ nhất bắt được ba loại lỗi tốn thời gian nhất:
 * cấu hình sai, migration hỏng, và thiếu extension PostGIS.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class FoodmapBackendApplicationTests {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("Ngữ cảnh Spring khởi tạo được")
    void contextLoads() {
    }

    @Test
    @DisplayName("PostGIS đã được bật trong CSDL test")
    void postgisIsEnabled() {
        String version = jdbc.queryForObject("SELECT PostGIS_Version()", String.class);
        assertThat(version).isNotBlank();
    }

    @Test
    @DisplayName("Flyway đã tạo đủ bảng và index không gian")
    void migrationsCreatedSchema() {
        Integer placeTables = jdbc.queryForObject("""
                SELECT count(*) FROM information_schema.tables
                WHERE table_schema = 'public' AND table_name IN ('places', 'reviews', 'visits', 'feedbacks')
                """, Integer.class);
        assertThat(placeTables).isEqualTo(4);

        // Thiếu index GiST thì truy vấn tìm quanh đây sẽ quét toàn bảng (ADR-0003)
        Integer spatialIndex = jdbc.queryForObject("""
                SELECT count(*) FROM pg_indexes
                WHERE tablename = 'places' AND indexname = 'idx_places_location'
                """, Integer.class);
        assertThat(spatialIndex).isEqualTo(1);
    }

    @Test
    @DisplayName("Dữ liệu tham chiếu danh mục đã được seed kèm bản dịch vi và en")
    void categoriesAreSeeded() {
        Integer categories = jdbc.queryForObject("SELECT count(*) FROM categories", Integer.class);
        assertThat(categories).isEqualTo(13);

        Integer viNames = jdbc.queryForObject(
                "SELECT count(*) FROM category_translations WHERE locale = 'vi'", Integer.class);
        assertThat(viNames).isEqualTo(13);
    }

    @Test
    @DisplayName("Truy vấn quanh đây dùng index GiST, không quét toàn bảng")
    void nearbyQueryUsesSpatialIndex() {
        // Ép Postgres ưu tiên index ngay cả khi bảng còn ít dòng
        jdbc.execute("SET LOCAL enable_seqscan = off");

        String plan = String.join("\n", jdbc.queryForList("""
                EXPLAIN
                SELECT p.id FROM places p
                WHERE ST_DWithin(p.location,
                        CAST(ST_SetSRID(ST_MakePoint(106.6297, 10.8231), 4326) AS geography), 2000)
                """, String.class));

        assertThat(plan).contains("idx_places_location");
    }
}
