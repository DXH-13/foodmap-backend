package com.foodmap.place.infrastructure;

import com.foodmap.place.domain.Place;
import com.foodmap.place.domain.PlaceDetailsRow;
import com.foodmap.place.domain.PlaceSummaryProjection;
import com.foodmap.place.domain.PlaceSummaryRow;
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
            FROM place.places p
            LEFT JOIN place.place_translations t   ON t.place_id = p.id   AND t.locale = :locale
            LEFT JOIN place.place_translations tvi ON tvi.place_id = p.id AND tvi.locale = 'vi'
            WHERE p.deleted_at IS NULL
              AND p.status IN ('PUBLISHED', 'TEMPORARILY_CLOSED')
              AND ST_DWithin(p.location,
                    CAST(ST_SetSRID(ST_MakePoint(:lng, :lat), 4326) AS geography),
                    :radiusMeters)
            ORDER BY p.location <-> CAST(ST_SetSRID(ST_MakePoint(:lng, :lat), 4326) AS geography)
            """,
            countQuery = """
            SELECT count(*)
            FROM place.places p
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

    /**
     * Tìm địa điểm theo từ khoá trên tên và mô tả, không phân biệt hoa thường và
     * không phân biệt dấu (dùng {@code f_unaccent} — xem V1, hàm đã bọc thành
     * IMMUTABLE để dùng được trong index).
     *
     * <p>Thêm cho tool {@code search_places} của chatbot ở Bước 5. Từ khoá phải
     * được escape ký tự {@code %}, {@code _}, {@code \} trước khi truyền vào
     * (xem {@code PlaceService#searchByKeyword}) vì ILIKE xem chúng là ký tự
     * đại diện. Chỉ trả địa điểm đang hiển thị công khai; {@code distanceMeters}
     * luôn là 0 vì tìm theo từ khoá không có điểm gốc để đo khoảng cách.
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
                   0.0                                         AS distanceMeters
            FROM place.places p
            LEFT JOIN place.place_translations t   ON t.place_id = p.id   AND t.locale = :locale
            LEFT JOIN place.place_translations tvi ON tvi.place_id = p.id AND tvi.locale = 'vi'
            WHERE p.deleted_at IS NULL
              AND p.status IN ('PUBLISHED', 'TEMPORARILY_CLOSED')
              AND (f_unaccent(COALESCE(t.name, tvi.name)) ILIKE '%' || f_unaccent(:keyword) || '%'
                OR f_unaccent(COALESCE(t.description, tvi.description)) ILIKE '%' || f_unaccent(:keyword) || '%')
            ORDER BY p.average_rating DESC NULLS LAST, p.review_count DESC
            """,
            countQuery = """
            SELECT count(*)
            FROM place.places p
            LEFT JOIN place.place_translations t   ON t.place_id = p.id   AND t.locale = :locale
            LEFT JOIN place.place_translations tvi ON tvi.place_id = p.id AND tvi.locale = 'vi'
            WHERE p.deleted_at IS NULL
              AND p.status IN ('PUBLISHED', 'TEMPORARILY_CLOSED')
              AND (f_unaccent(COALESCE(t.name, tvi.name)) ILIKE '%' || f_unaccent(:keyword) || '%'
                OR f_unaccent(COALESCE(t.description, tvi.description)) ILIKE '%' || f_unaccent(:keyword) || '%')
            """,
            nativeQuery = true)
    Page<PlaceSummaryProjection> searchByKeyword(@Param("keyword") String keyword,
                                                 @Param("locale") String locale,
                                                 Pageable pageable);

    /**
     * Thông tin tối thiểu của một địa điểm cho module khác (đánh giá, ghé thăm).
     * Tên lấy theo {@code :locale}, thiếu thì lùi về bản tiếng Việt.
     */
    @Query(value = """
            SELECT p.id                                        AS id,
                   COALESCE(t.name, tvi.name)                  AS name,
                   ST_Y(CAST(p.location AS geometry))          AS latitude,
                   ST_X(CAST(p.location AS geometry))          AS longitude,
                   p.status                                    AS status
            FROM place.places p
            LEFT JOIN place.place_translations t   ON t.place_id = p.id   AND t.locale = :locale
            LEFT JOIN place.place_translations tvi ON tvi.place_id = p.id AND tvi.locale = 'vi'
            WHERE p.id = :id AND p.deleted_at IS NULL
            """,
            nativeQuery = true)
    Optional<PlaceDetailsRow> findDetailsById(@Param("id") UUID id, @Param("locale") String locale);

    /**
     * Khoảng cách (mét) từ một toạ độ tới địa điểm, tính bằng PostGIS trên cột
     * geography. Dùng cho luật check-in trong 200m (FR-VISIT-02).
     */
    @Query(value = """
            SELECT ST_Distance(p.location,
                    CAST(ST_SetSRID(ST_MakePoint(:lng, :lat), 4326) AS geography))
            FROM place.places p
            WHERE p.id = :id AND p.deleted_at IS NULL
            """,
            nativeQuery = true)
    Optional<Double> distanceMetersTo(@Param("id") UUID id,
                                      @Param("lat") double latitude,
                                      @Param("lng") double longitude);

    /**
     * Địa điểm rút gọn cho một danh sách id (danh sách yêu thích). Chỉ trả những
     * địa điểm còn tồn tại; thứ tự trả về không đảm bảo.
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
                   ST_X(CAST(p.location AS geometry))          AS longitude
            FROM place.places p
            LEFT JOIN place.place_translations t   ON t.place_id = p.id   AND t.locale = :locale
            LEFT JOIN place.place_translations tvi ON tvi.place_id = p.id AND tvi.locale = 'vi'
            WHERE p.id IN :ids AND p.deleted_at IS NULL
            """,
            nativeQuery = true)
    java.util.List<PlaceSummaryRow> findSummariesByIds(@Param("ids") java.util.List<UUID> ids,
                                                       @Param("locale") String locale);
}
