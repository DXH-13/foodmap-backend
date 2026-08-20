package com.foodmap.place;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PlaceRepository extends JpaRepository<Place, UUID> {

    Optional<Place> findByIdAndDeletedAtIsNull(UUID id);

    Optional<Place> findBySlugAndDeletedAtIsNull(String slug);

    /**
     * Tìm địa điểm quanh một toạ độ, sắp xếp theo khoảng cách tăng dần.
     *
     * <p>Ba điểm quan trọng, đừng sửa nếu chưa đọc docs/SDD/du-lieu/geo-model.md:
     * <ul>
     *   <li>{@code ST_DWithin} dùng được index GiST. Viết
     *       {@code ST_Distance(...) < :radius} trong {@code WHERE} thì <b>không</b> —
     *       kết quả vẫn đúng nhưng chậm gấp hàng chục lần.</li>
     *   <li>{@code ST_MakePoint} nhận <b>kinh độ trước</b>: {@code (:lng, :lat)}.</li>
     *   <li>Toán tử KNN {@code <->} sắp xếp theo khoảng cách và cũng dùng index.</li>
     * </ul>
     *
     * <p>Dùng {@code CAST(... AS geography)} thay vì cú pháp {@code ::geography} vì
     * dấu hai chấm đôi xung đột với cách Hibernate phân tích tham số {@code :name}.
     *
     * <p>Tên hiển thị lấy theo {@code :locale}, thiếu thì lùi về bản tiếng Việt
     * — không bao giờ trả {@code null} (FR-I18N-03).
     */
    @Query(value = """
            SELECT p.id                                        AS id,
                   p.slug                                      AS slug,
                   p.place_type                                AS placeType,
                   p.status                                    AS status,
                   COALESCE(t.name, tvi.name)                  AS name,
                   p.address                                   AS address,
                   p.average_rating                            AS averageRating,
                   p.review_count                              AS reviewCount,
                   p.visit_count                               AS visitCount,
                   ST_Y(CAST(p.location AS geometry))          AS latitude,
                   ST_X(CAST(p.location AS geometry))          AS longitude,
                   ST_Distance(p.location,
                       CAST(ST_SetSRID(ST_MakePoint(:lng, :lat), 4326) AS geography)) AS distanceMeters
            FROM places p
            LEFT JOIN place_translations t   ON t.place_id = p.id   AND t.locale = :locale
            LEFT JOIN place_translations tvi ON tvi.place_id = p.id AND tvi.locale = 'vi'
            WHERE p.deleted_at IS NULL
              AND p.status IN ('PUBLISHED', 'TEMPORARILY_CLOSED')
              AND ST_DWithin(p.location,
                    CAST(ST_SetSRID(ST_MakePoint(:lng, :lat), 4326) AS geography),
                    :radiusMeters)
            ORDER BY p.location <-> CAST(ST_SetSRID(ST_MakePoint(:lng, :lat), 4326) AS geography)
            """,
            countQuery = """
            SELECT count(*)
            FROM places p
            WHERE p.deleted_at IS NULL
              AND p.status IN ('PUBLISHED', 'TEMPORARILY_CLOSED')
              AND ST_DWithin(p.location,
                    CAST(ST_SetSRID(ST_MakePoint(:lng, :lat), 4326) AS geography),
                    :radiusMeters)
            """,
            nativeQuery = true)
    Page<PlaceSummaryProjection> findNearby(@Param("lat") double latitude,
                                            @Param("lng") double longitude,
                                            @Param("radiusMeters") int radiusMeters,
                                            @Param("locale") String locale,
                                            Pageable pageable);
}
