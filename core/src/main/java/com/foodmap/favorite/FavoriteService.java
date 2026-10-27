package com.foodmap.favorite;

import com.foodmap.common.exception.NotFoundException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Thêm/bỏ yêu thích (PLACE-04). Bookmark thuần, không có trạng thái trung gian.
 * Đọc danh sách nằm ở {@code PlaceService.listFavorites} vì kết quả là {@code PlaceSummary}.
 */
@Service
public class FavoriteService {

    private final JdbcClient jdbc;

    public FavoriteService(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Idempotent: đã yêu thích rồi thì không làm gì. Địa điểm phải tồn tại và không phải bản nháp
     * — cùng điều kiện với danh sách yêu thích, nên quán đã đóng cửa vẫn thêm được (hiển thị kèm nhãn).
     */
    @Transactional
    public void add(UUID userId, UUID placeId) {
        int inserted = jdbc.sql("""
                        INSERT INTO favorites (user_id, place_id)
                        SELECT :userId, p.id FROM places p
                        WHERE p.id = :placeId AND p.deleted_at IS NULL AND p.status <> 'DRAFT'
                        ON CONFLICT (user_id, place_id) DO NOTHING
                        """)
                .param("userId", userId)
                .param("placeId", placeId)
                .update();
        if (inserted == 0 && !exists(userId, placeId)) {
            throw new NotFoundException("PLACE_NOT_FOUND", "place.error.not_found");
        }
    }

    /** Không có trong danh sách thì cũng coi như đã bỏ — spec chỉ khai 204. */
    @Transactional
    public void remove(UUID userId, UUID placeId) {
        jdbc.sql("DELETE FROM favorites WHERE user_id = :userId AND place_id = :placeId")
                .param("userId", userId)
                .param("placeId", placeId)
                .update();
    }

    private boolean exists(UUID userId, UUID placeId) {
        return jdbc.sql("SELECT EXISTS (SELECT 1 FROM favorites WHERE user_id = :userId AND place_id = :placeId)")
                .param("userId", userId)
                .param("placeId", placeId)
                .query(Boolean.class).single();
    }
}
