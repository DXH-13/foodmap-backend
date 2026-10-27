package com.foodmap;

import com.foodmap.auth.JwtService;
import org.junit.jupiter.api.BeforeEach;
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

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Bảng tin đánh giá và nhóm /me (hồ sơ, đánh giá, yêu thích, lượt đến). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class MeAndReviewFeedControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private JwtService jwtService;

    private UUID me;
    private UUID other;
    private UUID place;

    @BeforeEach
    void setUp() {
        me = TestData.user(jdbc, "Tôi");
        other = TestData.user(jdbc, "Người khác");
        place = TestData.place(jdbc, "PUBLISHED", "Bún chả thử", null, 4.0, 1);
    }

    private MockHttpServletRequestBuilder authed(String path) {
        return get(path).header("Authorization", "Bearer " + jwtService.issueAccessToken(me, "USER", true));
    }

    @Test
    @DisplayName("Bảng tin: chỉ APPROVED của địa điểm công khai, có place, không lộ moderationNote")
    void feedShowsOnlyApproved() throws Exception {
        UUID approved = TestData.review(jdbc, place, other, 4, "APPROVED", "ghi chú nội bộ");
        UUID pending = TestData.review(jdbc, place, me, 5, "PENDING", null);
        UUID draftPlace = TestData.place(jdbc, "DRAFT", "Quán nháp", null, null, 0);
        UUID onDraft = TestData.review(jdbc, draftPlace, other, 3, "APPROVED", null);

        // Không cần token; trang đủ lớn để chứa dữ liệu của các test khác chạy trước
        mockMvc.perform(get("/api/v1/reviews/feed").param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id", hasItem(approved.toString())))
                .andExpect(jsonPath("$.content[*].id", not(hasItem(pending.toString()))))
                .andExpect(jsonPath("$.content[*].id", not(hasItem(onDraft.toString()))))
                .andExpect(jsonPath("$.content[*].status", not(hasItem("PENDING"))))
                .andExpect(jsonPath("$.content[?(@.id == '%s')].place.name".formatted(approved)).value("Bún chả thử"))
                .andExpect(jsonPath("$.content[?(@.id == '%s')].place.placeType".formatted(approved)).value("RESTAURANT"))
                .andExpect(jsonPath("$.content[?(@.id == '%s')].author.displayName".formatted(approved)).value("Người khác"))
                .andExpect(jsonPath("$.content[?(@.id == '%s')].moderationNote".formatted(approved)).value((Object) null))
                .andExpect(jsonPath("$.content[?(@.id == '%s')].media".formatted(approved)).isArray());
    }

    @Test
    @DisplayName("Bảng tin: phân trang sai trả 400")
    void feedValidatesPaging() throws Exception {
        mockMvc.perform(get("/api/v1/reviews/feed").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("Mọi endpoint /me trả 401 khi thiếu token hoặc token hỏng")
    void meEndpointsRequireAuthentication() throws Exception {
        for (String path : new String[]{"/api/v1/me", "/api/v1/me/reviews", "/api/v1/me/favorites", "/api/v1/me/visits"}) {
            mockMvc.perform(get(path))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
            mockMvc.perform(get(path).header("Authorization", "Bearer token-gia"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test
    @DisplayName("GET /me trả hồ sơ của chính người gọi")
    void getCurrentUser() throws Exception {
        mockMvc.perform(authed("/api/v1/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(me.toString()))
                .andExpect(jsonPath("$.displayName").value("Tôi"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("GET /me/reviews: mọi trạng thái của chính mình, kèm moderationNote và place")
    void listOwnReviews() throws Exception {
        UUID rejected = TestData.review(jdbc, place, me, 1, "REJECTED", "Nội dung quảng cáo");
        TestData.review(jdbc, place, other, 5, "APPROVED", null);

        mockMvc.perform(authed("/api/v1/me/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(rejected.toString()))
                .andExpect(jsonPath("$.content[0].status").value("REJECTED"))
                .andExpect(jsonPath("$.content[0].moderationNote").value("Nội dung quảng cáo"))
                .andExpect(jsonPath("$.content[0].place.id").value(place.toString()));
    }

    @Test
    @DisplayName("GET /me/favorites: mới thêm trước; DISTANCE có khoảng cách; thiếu toạ độ thì 400")
    void listFavorites() throws Exception {
        UUID older = TestData.place(jdbc, "PERMANENTLY_CLOSED", "Quán đã đóng", null, null, 0);
        jdbc.update("INSERT INTO favorites (user_id, place_id, created_at) VALUES (?, ?, now() - interval '2 days')", me, older);
        jdbc.update("INSERT INTO favorites (user_id, place_id) VALUES (?, ?)", me, place);
        jdbc.update("INSERT INTO favorites (user_id, place_id) VALUES (?, ?)", other, place);

        mockMvc.perform(authed("/api/v1/me/favorites"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].id").value(place.toString()))
                .andExpect(jsonPath("$.content[0].distanceMeters").value((Object) null))
                // Quán đóng cửa vĩnh viễn vẫn nằm trong danh sách, kèm nhãn trạng thái
                .andExpect(jsonPath("$.content[1].status").value("PERMANENTLY_CLOSED"));

        mockMvc.perform(authed("/api/v1/me/favorites").param("sort", "DISTANCE")
                        .param("latitude", "10.78").param("longitude", "106.70"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].distanceMeters").value(0.0));

        mockMvc.perform(authed("/api/v1/me/favorites").param("sort", "DISTANCE").param("latitude", "10.78")
                        .header("Accept-Language", "en"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COORDINATES_REQUIRED"))
                .andExpect(jsonPath("$.message").value("Your current location is required to sort by distance."));
    }

    @Test
    @DisplayName("GET /me/visits: lượt đến của chính mình, có tên địa điểm; phân trang sai trả 400")
    void listOwnVisits() throws Exception {
        jdbc.update("""
                INSERT INTO visits (user_id, place_id, visit_date, recorded_location, distance_meters)
                VALUES (?, ?, DATE '2026-10-20', CAST(ST_SetSRID(ST_MakePoint(106.70, 10.7801), 4326) AS geography), 11)
                """, me, place);

        mockMvc.perform(authed("/api/v1/me/visits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].placeId").value(place.toString()))
                .andExpect(jsonPath("$.content[0].placeName").value("Bún chả thử"))
                .andExpect(jsonPath("$.content[0].visitDate").value("2026-10-20"))
                .andExpect(jsonPath("$.content[0].distanceMeters").value(11));

        mockMvc.perform(authed("/api/v1/me/visits").param("size", "0"))
                .andExpect(status().isBadRequest());
    }
}
