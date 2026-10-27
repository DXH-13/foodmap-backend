package com.foodmap;

import com.foodmap.auth.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.UUID;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** GET /api/v1/places/{placeId} (PLACE-01) và bộ lọc của GET /api/v1/places/nearby (MAP, DISCOVERY-01). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class PlaceDetailAndNearbyControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private JwtService jwtService;

    // ─────────────────────────── chi tiết ───────────────────────────

    @Test
    @DisplayName("Chi tiết cho khách: đủ trường bắt buộc, chưa có đánh giá là null, không có trường cá nhân")
    void detailForGuest() throws Exception {
        UUID place = TestData.place(jdbc, "PUBLISHED", "Phở Chi Tiết", "Nước dùng trong", null, 0);
        UUID category = TestData.category(jdbc, TestData.token(), "Danh mục chi tiết");
        TestData.tag(jdbc, place, category);
        jdbc.update("UPDATE places SET phone = '0909000000' WHERE id = ?", place);
        jdbc.update("""
                INSERT INTO opening_hours (place_id, day_of_week, open_time, close_time, is_closed_all_day)
                VALUES (?, 1, '06:00', '10:30', FALSE), (?, 2, NULL, NULL, TRUE)
                """, place, place);
        jdbc.update("""
                INSERT INTO place_media (place_id, storage_key, display_key, thumbnail_key, width, height, size_bytes, is_cover)
                VALUES (?, 'goc.jpg', 'hien-thi.jpg', 'nho.jpg', 1600, 1200, 1000, TRUE)
                """, place);

        mockMvc.perform(get("/api/v1/places/" + place))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(place.toString()))
                .andExpect(jsonPath("$.name").value("Phở Chi Tiết"))
                .andExpect(jsonPath("$.description").value("Nước dùng trong"))
                .andExpect(jsonPath("$.phone").value("0909000000"))
                .andExpect(jsonPath("$.placeType").value("RESTAURANT"))
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.coordinates.latitude").value(10.78))
                .andExpect(jsonPath("$.coordinates.longitude").value(106.70))
                .andExpect(jsonPath("$.averageRating").value((Object) null))
                .andExpect(jsonPath("$.reviewCount").value(0))
                .andExpect(jsonPath("$.visitCount").value(0))
                .andExpect(jsonPath("$.distinctVisitorCount").value(0))
                .andExpect(jsonPath("$.categories[0].name").value("Danh mục chi tiết"))
                .andExpect(jsonPath("$.openingHours.length()").value(2))
                .andExpect(jsonPath("$.openingHours[0].dayOfWeek").value(1))
                .andExpect(jsonPath("$.openingHours[0].openTime").value("06:00"))
                .andExpect(jsonPath("$.openingHours[0].closeTime").value("10:30"))
                .andExpect(jsonPath("$.openingHours[0].isClosedAllDay").value(false))
                .andExpect(jsonPath("$.openingHours[1].openTime").value((Object) null))
                .andExpect(jsonPath("$.openingHours[1].isClosedAllDay").value(true))
                .andExpect(jsonPath("$.isOpenNow").isBoolean())
                .andExpect(jsonPath("$.images[0].mediaType").value("IMAGE"))
                .andExpect(jsonPath("$.images[0].url").value(org.hamcrest.Matchers.endsWith("/hien-thi.jpg")))
                .andExpect(jsonPath("$.images[0].thumbnailUrl").value(org.hamcrest.Matchers.endsWith("/nho.jpg")))
                .andExpect(jsonPath("$.coverImageUrl").value(org.hamcrest.Matchers.endsWith("/hien-thi.jpg")))
                .andExpect(jsonPath("$.isFavorited").value((Object) null))
                .andExpect(jsonPath("$.ownVisitCount").value((Object) null))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    @DisplayName("Chi tiết dịch theo Accept-Language, thiếu bản en thì lùi về vi")
    void detailIsTranslated() throws Exception {
        UUID translated = TestData.place(jdbc, "PUBLISHED", "Bún bò", "Cay", null, 0);
        jdbc.update("INSERT INTO place_translations (place_id, locale, name, description) VALUES (?, 'en', 'Hue beef noodle', 'Spicy')",
                translated);
        UUID viOnly = TestData.place(jdbc, "TEMPORARILY_CLOSED", "Chỉ tiếng Việt", null, 4.5, 2);

        mockMvc.perform(get("/api/v1/places/" + translated).header("Accept-Language", "en"))
                .andExpect(jsonPath("$.name").value("Hue beef noodle"))
                .andExpect(jsonPath("$.description").value("Spicy"));

        mockMvc.perform(get("/api/v1/places/" + viOnly).header("Accept-Language", "en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Chỉ tiếng Việt"))
                .andExpect(jsonPath("$.averageRating").value(4.5))
                .andExpect(jsonPath("$.isOpenNow").value((Object) null))
                .andExpect(jsonPath("$.openingHours.length()").value(0))
                .andExpect(jsonPath("$.images.length()").value(0));
    }

    @Test
    @DisplayName("Có token: điền isFavorited và ownVisitCount của chính người gọi")
    void detailForSignedInUser() throws Exception {
        UUID place = TestData.place(jdbc, "PUBLISHED", "Quán quen", null, null, 0);
        UUID me = TestData.user(jdbc, "Tôi");
        UUID other = TestData.user(jdbc, "Người khác");
        jdbc.update("INSERT INTO favorites (user_id, place_id) VALUES (?, ?)", me, place);
        for (Object[] v : new Object[][]{{me, "2026-01-01"}, {me, "2026-01-02"}, {other, "2026-01-01"}}) {
            jdbc.update("""
                    INSERT INTO visits (user_id, place_id, visit_date, recorded_location, distance_meters)
                    VALUES (?, ?, CAST(? AS date), CAST(ST_SetSRID(ST_MakePoint(106.70, 10.78), 4326) AS geography), 5)
                    """, v[0], place, v[1]);
        }

        mockMvc.perform(asUser(get("/api/v1/places/" + place), me))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isFavorited").value(true))
                .andExpect(jsonPath("$.ownVisitCount").value(2));

        mockMvc.perform(asUser(get("/api/v1/places/" + place), other))
                .andExpect(jsonPath("$.isFavorited").value(false))
                .andExpect(jsonPath("$.ownVisitCount").value(1));

        // Token hỏng không chặn endpoint công khai — chỉ coi như khách
        mockMvc.perform(get("/api/v1/places/" + place).header("Authorization", "Bearer token-gia"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isFavorited").value((Object) null));
    }

    @Test
    @DisplayName("Không tồn tại, bản nháp, đã xoá đều trả 404 PLACE_NOT_FOUND")
    void detailNotFound() throws Exception {
        UUID draft = TestData.place(jdbc, "DRAFT", "Nháp", null, null, 0);
        UUID deleted = TestData.place(jdbc, "PUBLISHED", "Đã xoá", null, null, 0);
        jdbc.update("UPDATE places SET deleted_at = now() WHERE id = ?", deleted);

        for (UUID id : new UUID[]{UUID.randomUUID(), draft, deleted}) {
            mockMvc.perform(get("/api/v1/places/" + id))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("PLACE_NOT_FOUND"))
                    .andExpect(jsonPath("$.message").value("Không tìm thấy địa điểm."))
                    .andExpect(jsonPath("$.traceId").isNotEmpty());
        }
    }

    @Test
    @DisplayName("Quán đóng cửa vĩnh viễn vẫn xem được chi tiết và review, kèm trạng thái để hiện nhãn")
    void permanentlyClosedDetailIsVisible() throws Exception {
        UUID closed = TestData.place(jdbc, "PERMANENTLY_CLOSED", "Đóng hẳn", null, null, 0);

        mockMvc.perform(get("/api/v1/places/" + closed))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PERMANENTLY_CLOSED"));
        mockMvc.perform(get("/api/v1/places/" + closed + "/reviews"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("placeId không phải UUID trả 400 VALIDATION_FAILED, không phải 500")
    void detailRejectsMalformedId() throws Exception {
        mockMvc.perform(get("/api/v1/places/khong-phai-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    // ─────────────────────────── nearby ───────────────────────────

    @Test
    @DisplayName("nearby lọc theo categorySlugs (một hoặc nhiều) và placeType")
    void nearbyFiltersByCategoryAndType() throws Exception {
        String slugA = TestData.token();
        String slugB = TestData.token();
        UUID catA = TestData.category(jdbc, slugA, "A");
        UUID catB = TestData.category(jdbc, slugB, "B");
        UUID inA = TestData.place(jdbc, "PUBLISHED", "Thuộc A", null, null, 0);
        UUID inB = TestData.place(jdbc, "PUBLISHED", "Thuộc B", null, null, 0);
        TestData.place(jdbc, "PUBLISHED", "Không thuộc gì", null, null, 0);
        TestData.tag(jdbc, inA, catA);
        TestData.tag(jdbc, inB, catB);
        jdbc.update("UPDATE places SET place_type = 'STREET_FOOD' WHERE id = ?", inB);

        mockMvc.perform(nearby().param("categorySlugs", slugA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(inA.toString()));

        mockMvc.perform(nearby().param("categorySlugs", slugA, slugB))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[*].id", containsInAnyOrder(inA.toString(), inB.toString())));

        mockMvc.perform(nearby().param("categorySlugs", slugA, slugB).param("placeType", "STREET_FOOD"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(inB.toString()));
    }

    @Test
    @DisplayName("nearby lọc minRating (chưa có đánh giá bị loại) và openNow")
    void nearbyFiltersByRatingAndOpenNow() throws Exception {
        String slug = TestData.token();
        UUID cat = TestData.category(jdbc, slug, "Lọc");
        UUID high = TestData.place(jdbc, "PUBLISHED", "Điểm cao", null, 4.5, 3);
        UUID low = TestData.place(jdbc, "PUBLISHED", "Điểm thấp", null, 3.0, 3);
        UUID unrated = TestData.place(jdbc, "PUBLISHED", "Chưa chấm", null, null, 0);
        UUID closedToday = TestData.place(jdbc, "PUBLISHED", "Nghỉ", null, null, 0);
        UUID tempClosed = TestData.place(jdbc, "TEMPORARILY_CLOSED", "Tạm đóng", null, null, 0);
        for (UUID id : new UUID[]{high, low, unrated, closedToday, tempClosed}) {
            TestData.tag(jdbc, id, cat);
        }
        for (int day = 1; day <= 7; day++) {
            // open_time = close_time là khoảng qua nửa đêm trọn 24 giờ — luôn mở, test không phụ thuộc giờ chạy
            for (UUID id : new UUID[]{unrated, tempClosed}) {
                jdbc.update("""
                        INSERT INTO opening_hours (place_id, day_of_week, open_time, close_time, is_closed_all_day)
                        VALUES (?, ?, '00:00', '00:00', FALSE)
                        """, id, day);
            }
            jdbc.update("""
                    INSERT INTO opening_hours (place_id, day_of_week, open_time, close_time, is_closed_all_day)
                    VALUES (?, ?, NULL, NULL, TRUE)
                    """, closedToday, day);
        }

        mockMvc.perform(nearby().param("categorySlugs", slug).param("minRating", "4"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(high.toString()));

        mockMvc.perform(nearby().param("categorySlugs", slug).param("openNow", "true"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(unrated.toString()));

        mockMvc.perform(nearby().param("categorySlugs", slug))
                .andExpect(jsonPath("$.totalElements").value(5));
    }

    @Test
    @DisplayName("nearby: tham số lọc sai trả 400")
    void nearbyRejectsInvalidFilters() throws Exception {
        mockMvc.perform(nearby().param("minRating", "6"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        mockMvc.perform(nearby().param("placeType", "BAR"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(nearby().param("radiusMeters", "50001"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("RADIUS_OUT_OF_RANGE"));
    }

    private static MockHttpServletRequestBuilder nearby() {
        return get("/api/v1/places/nearby").param("latitude", "10.78").param("longitude", "106.70");
    }

    private MockHttpServletRequestBuilder asUser(MockHttpServletRequestBuilder request, UUID userId) {
        return request.header("Authorization", "Bearer " + jwtService.issueAccessToken(userId, "USER", true));
    }
}
