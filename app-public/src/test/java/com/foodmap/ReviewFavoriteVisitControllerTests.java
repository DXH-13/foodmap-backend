package com.foodmap;

import com.foodmap.auth.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * GET /places/{placeId}/reviews, PUT/DELETE /me/favorites/{placeId}, POST /places/{placeId}/visits.
 * Địa điểm của {@link TestData#place} nằm ở (10.78, 106.70).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class ReviewFavoriteVisitControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private JwtService jwtService;

    // ─────────────────────────── reviews ───────────────────────────

    @Test
    @DisplayName("Review của quán: khách chỉ thấy APPROVED; tác giả thấy thêm review của mình kèm trạng thái và lý do")
    void placeReviewsVisibility() throws Exception {
        UUID place = TestData.place(jdbc, "PUBLISHED", "Quán review", null, null, 0);
        UUID a = TestData.user(jdbc, "A");
        UUID b = TestData.user(jdbc, "B");
        UUID c = TestData.user(jdbc, "C");
        UUID approved = TestData.review(jdbc, place, a, 5, "APPROVED", null);
        UUID pendingOfB = TestData.review(jdbc, place, b, 2, "PENDING", null);
        UUID rejectedOfC = TestData.review(jdbc, place, c, 1, "REJECTED", "Nội dung không phù hợp");

        mockMvc.perform(get("/api/v1/places/" + place + "/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(approved.toString()))
                .andExpect(jsonPath("$.content[0].author.displayName").value("A"))
                .andExpect(jsonPath("$.content[0].status").value("APPROVED"))
                .andExpect(jsonPath("$.content[0].media").isArray())
                .andExpect(jsonPath("$.content[0].place").value((Object) null))
                .andExpect(jsonPath("$.content[0].moderationNote").value((Object) null));

        mockMvc.perform(asUser(get("/api/v1/places/" + place + "/reviews"), b))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[*].id", containsInAnyOrder(approved.toString(), pendingOfB.toString())));

        mockMvc.perform(asUser(get("/api/v1/places/" + place + "/reviews"), c))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[?(@.id == '" + rejectedOfC + "')].moderationNote")
                        .value("Nội dung không phù hợp"));

        mockMvc.perform(asUser(get("/api/v1/places/" + place + "/reviews").param("sort", "LOWEST_RATING"), b))
                .andExpect(jsonPath("$.content[0].id").value(pendingOfB.toString()));
        mockMvc.perform(asUser(get("/api/v1/places/" + place + "/reviews").param("sort", "HIGHEST_RATING"), b))
                .andExpect(jsonPath("$.content[0].id").value(approved.toString()));
    }

    @Test
    @DisplayName("Review của quán không tồn tại / bản nháp → 404; sort lạ → 400")
    void placeReviewsErrors() throws Exception {
        UUID draft = TestData.place(jdbc, "DRAFT", "Nháp", null, null, 0);
        for (UUID id : new UUID[]{UUID.randomUUID(), draft}) {
            mockMvc.perform(get("/api/v1/places/" + id + "/reviews"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("PLACE_NOT_FOUND"));
        }
        UUID place = TestData.place(jdbc, "PUBLISHED", "Quán", null, null, 0);
        mockMvc.perform(get("/api/v1/places/" + place + "/reviews").param("sort", "OLDEST"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    // ─────────────────────────── favorites ───────────────────────────

    @Test
    @DisplayName("PUT yêu thích idempotent (200 hai lần, một bản ghi); DELETE trả 204 kể cả khi chưa có")
    void addAndRemoveFavorite() throws Exception {
        UUID place = TestData.place(jdbc, "PUBLISHED", "Quán thích", null, null, 0);
        UUID me = TestData.user(jdbc, "Tôi");

        mockMvc.perform(asUser(put("/api/v1/me/favorites/" + place), me)).andExpect(status().isOk());
        mockMvc.perform(asUser(put("/api/v1/me/favorites/" + place), me)).andExpect(status().isOk());
        assertThat(favoriteCount(me, place)).isEqualTo(1);

        mockMvc.perform(asUser(get("/api/v1/places/" + place), me))
                .andExpect(jsonPath("$.isFavorited").value(true));

        mockMvc.perform(asUser(delete("/api/v1/me/favorites/" + place), me)).andExpect(status().isNoContent());
        mockMvc.perform(asUser(delete("/api/v1/me/favorites/" + place), me)).andExpect(status().isNoContent());
        assertThat(favoriteCount(me, place)).isZero();
    }

    @Test
    @DisplayName("Yêu thích quán không tồn tại / bản nháp → 404; id sai → 400; không token → 401")
    void favoriteErrors() throws Exception {
        UUID me = TestData.user(jdbc, "Tôi");
        UUID draft = TestData.place(jdbc, "DRAFT", "Nháp", null, null, 0);
        for (UUID id : new UUID[]{UUID.randomUUID(), draft}) {
            mockMvc.perform(asUser(put("/api/v1/me/favorites/" + id), me))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("PLACE_NOT_FOUND"));
        }
        mockMvc.perform(asUser(put("/api/v1/me/favorites/khong-phai-uuid"), me))
                .andExpect(status().isBadRequest());

        UUID place = TestData.place(jdbc, "PUBLISHED", "Quán", null, null, 0);
        mockMvc.perform(put("/api/v1/me/favorites/" + place)).andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/v1/me/favorites/" + place)).andExpect(status().isUnauthorized());
    }

    // ─────────────────────────── visits ───────────────────────────

    @Test
    @DisplayName("Ghi nhận lượt đến trong 200m: 201, ngày theo giờ VN, tăng visitCount; lần hai trong ngày → 409 VISIT_ALREADY_TODAY")
    void recordVisit() throws Exception {
        UUID place = TestData.place(jdbc, "PUBLISHED", "Quán đến", null, null, 0);
        UUID me = TestData.user(jdbc, "Tôi");

        // ~111m về phía bắc
        mockMvc.perform(visit(place, me, 10.781, 106.70))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.placeId").value(place.toString()))
                .andExpect(jsonPath("$.placeName").value("Quán đến"))
                .andExpect(jsonPath("$.visitDate").value(LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).toString()))
                .andExpect(jsonPath("$.distanceMeters").value(org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.greaterThan(100), org.hamcrest.Matchers.lessThan(120))))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());

        mockMvc.perform(visit(place, me, 10.78, 106.70))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VISIT_ALREADY_TODAY"));

        mockMvc.perform(asUser(get("/api/v1/places/" + place), me))
                .andExpect(jsonPath("$.visitCount").value(1))
                .andExpect(jsonPath("$.distinctVisitorCount").value(1))
                .andExpect(jsonPath("$.ownVisitCount").value(1));
    }

    @Test
    @DisplayName("Cách quá 200m → 409 VISIT_TOO_FAR kèm khoảng cách; không ghi bản ghi nào")
    void visitTooFar() throws Exception {
        UUID place = TestData.place(jdbc, "PUBLISHED", "Quán xa", null, null, 0);
        UUID me = TestData.user(jdbc, "Tôi");

        // ~221m về phía bắc (khoảng cách trên mặt cầu WGS-84)
        mockMvc.perform(visit(place, me, 10.782, 106.70))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VISIT_TOO_FAR"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("221m")));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM visits WHERE place_id = ?", Long.class, place)).isZero();
    }

    @Test
    @DisplayName("Lượt đến: quán không có → 404; thiếu/sai toạ độ → 400; không token → 401")
    void visitErrors() throws Exception {
        UUID me = TestData.user(jdbc, "Tôi");
        mockMvc.perform(visit(UUID.randomUUID(), me, 10.78, 106.70))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PLACE_NOT_FOUND"));

        UUID place = TestData.place(jdbc, "PUBLISHED", "Quán", null, null, 0);
        for (String body : new String[]{"{}", "{\"coordinates\":{\"latitude\":10.78}}",
                "{\"coordinates\":{\"latitude\":91,\"longitude\":106.70}}"}) {
            mockMvc.perform(asUser(post("/api/v1/places/" + place + "/visits"), me)
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        }

        mockMvc.perform(post("/api/v1/places/" + place + "/visits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"coordinates\":{\"latitude\":10.78,\"longitude\":106.70}}"))
                .andExpect(status().isUnauthorized());
    }

    private MockHttpServletRequestBuilder visit(UUID placeId, UUID userId, double lat, double lng) {
        return asUser(post("/api/v1/places/" + placeId + "/visits"), userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"coordinates\":{\"latitude\":" + lat + ",\"longitude\":" + lng + "}}");
    }

    private long favoriteCount(UUID userId, UUID placeId) {
        return jdbc.queryForObject("SELECT count(*) FROM favorites WHERE user_id = ? AND place_id = ?",
                Long.class, userId, placeId);
    }

    private MockHttpServletRequestBuilder asUser(MockHttpServletRequestBuilder request, UUID userId) {
        return request.header("Authorization", "Bearer " + jwtService.issueAccessToken(userId, "USER", true));
    }
}
