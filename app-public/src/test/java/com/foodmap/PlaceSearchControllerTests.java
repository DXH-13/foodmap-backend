package com.foodmap;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** GET /api/v1/places (FR-PLACE-04, DISCOVERY-03) và GET /api/v1/categories. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class PlaceSearchControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("Tìm không dấu, không phân biệt hoa thường: 'PHO' ra 'Phở'; khớp tên xếp trước khớp mô tả")
    void searchIgnoresAccentsAndRanksNameFirst() throws Exception {
        String token = TestData.token();
        UUID byDescription = TestData.place(jdbc, "PUBLISHED", "Quán Aaa", "Phở gánh gia truyền " + token, null, 0);
        UUID byName = TestData.place(jdbc, "PUBLISHED", "Phở Zzz " + token, null, null, 0);
        UUID draft = TestData.place(jdbc, "DRAFT", "Phở nháp " + token, null, null, 0);

        mockMvc.perform(get("/api/v1/places").param("query", "PHO " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].id").value(byName.toString()))
                .andExpect(jsonPath("$.content[0].name").value("Phở Zzz " + token))
                .andExpect(jsonPath("$.content[0].distanceMeters").value((Object) null))
                .andExpect(jsonPath("$.content[0].categories").isArray())
                .andExpect(jsonPath("$.content[1].id").value(byDescription.toString()))
                .andExpect(jsonPath("$.content[*].id", not(hasItem(draft.toString()))));
    }

    @Test
    @DisplayName("POPULAR: điểm cao trước, cùng điểm thì nhiều đánh giá trước, chưa có điểm xếp cuối; lọc theo danh mục")
    void popularSortsNullRatingLast() throws Exception {
        String slug = TestData.token();
        UUID category = TestData.category(jdbc, slug, "Danh mục thử");
        UUID unrated = TestData.place(jdbc, "PUBLISHED", "Chưa ai chấm", null, null, 0);
        UUID high = TestData.place(jdbc, "TEMPORARILY_CLOSED", "Điểm cao", null, 4.8, 3);
        UUID highMore = TestData.place(jdbc, "PUBLISHED", "Điểm cao nhiều lượt", null, 4.8, 10);
        UUID low = TestData.place(jdbc, "PUBLISHED", "Điểm thấp", null, 2.0, 5);
        TestData.place(jdbc, "PUBLISHED", "Không thuộc danh mục", null, 5.0, 50);
        for (UUID id : new UUID[]{unrated, high, highMore, low}) {
            TestData.tag(jdbc, id, category);
        }

        mockMvc.perform(get("/api/v1/places").param("sort", "POPULAR").param("categorySlugs", slug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(4))
                .andExpect(jsonPath("$.content[0].id").value(highMore.toString()))
                .andExpect(jsonPath("$.content[1].id").value(high.toString()))
                .andExpect(jsonPath("$.content[2].id").value(low.toString()))
                .andExpect(jsonPath("$.content[3].id").value(unrated.toString()))
                .andExpect(jsonPath("$.content[3].averageRating").value((Object) null))
                .andExpect(jsonPath("$.content[0].categories[0].slug").value(slug))
                .andExpect(jsonPath("$.content[0].categories[0].name").value("Danh mục thử"));
    }

    @Test
    @DisplayName("Tham số sai trả 400 VALIDATION_FAILED, không phải 500")
    void invalidParametersAreRejected() throws Exception {
        mockMvc.perform(get("/api/v1/places").param("sort", "NEWEST"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        mockMvc.perform(get("/api/v1/places").param("placeType", "BAR"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/places").param("query", "x".repeat(101)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/places").param("size", "101"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Danh mục: không cần đăng nhập, theo displayOrder, tên dịch theo Accept-Language")
    void listCategories() throws Exception {
        mockMvc.perform(get("/api/v1/categories").header("Accept-Language", "en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slug").value("pho"))
                .andExpect(jsonPath("$[0].name").value("Pho"))
                .andExpect(jsonPath("$[0].displayOrder").value(10));

        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(jsonPath("$[0].name").value("Phở"));
    }
}
