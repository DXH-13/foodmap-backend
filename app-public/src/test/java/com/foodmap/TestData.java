package com.foodmap;

import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

/**
 * Chèn dữ liệu thẳng bằng SQL cho integration test. Các test dùng chung một CSDL,
 * nên mọi slug/email đều kèm hậu tố ngẫu nhiên.
 */
final class TestData {

    private TestData() {
    }

    /** Chuỗi chỉ gồm chữ thường — làm "từ khoá riêng" của một test khi tìm kiếm toàn văn. */
    static String token() {
        return "zq" + UUID.randomUUID().toString().replaceAll("[^a-f]", "");
    }

    static UUID user(JdbcTemplate jdbc, String displayName) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO users (id, email, password_hash, display_name, role)
                VALUES (?, ?, 'khong-dung-de-dang-nhap', ?, 'USER')
                """, id, id + "@example.com", displayName);
        return id;
    }

    static UUID place(JdbcTemplate jdbc, String status, String nameVi, String descriptionVi,
                      Double averageRating, int reviewCount) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO places (id, slug, place_type, location, status, average_rating, review_count)
                VALUES (?, ?, 'RESTAURANT', CAST(ST_SetSRID(ST_MakePoint(106.70, 10.78), 4326) AS geography), ?, ?, ?)
                """, id, "p-" + id, status, averageRating, reviewCount);
        jdbc.update("INSERT INTO place_translations (place_id, locale, name, description) VALUES (?, 'vi', ?, ?)",
                id, nameVi, descriptionVi);
        return id;
    }

    static UUID category(JdbcTemplate jdbc, String slug, String nameVi) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO categories (id, slug, display_order) VALUES (?, ?, 999)", id, slug);
        jdbc.update("INSERT INTO category_translations (category_id, locale, name) VALUES (?, 'vi', ?)", id, nameVi);
        return id;
    }

    static void tag(JdbcTemplate jdbc, UUID placeId, UUID categoryId) {
        jdbc.update("INSERT INTO place_categories (place_id, category_id) VALUES (?, ?)", placeId, categoryId);
    }

    static UUID review(JdbcTemplate jdbc, UUID placeId, UUID userId, int rating, String status, String note) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO reviews (id, place_id, user_id, rating, content, status, moderation_note)
                VALUES (?, ?, ?, ?, 'Nội dung thử', ?, ?)
                """, id, placeId, userId, rating, status, note);
        return id;
    }
}
