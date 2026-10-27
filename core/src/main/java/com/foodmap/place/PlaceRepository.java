package com.foodmap.place;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/**
 * Ghi chú chung cho các native query bên dưới:
 * <ul>
 *   <li>Dùng {@code CAST(... AS geography)} thay vì {@code ::geography} vì dấu hai chấm đôi
 *       xung đột với cách Hibernate phân tích tham số {@code :name}.</li>
 *   <li>{@code ST_MakePoint} nhận <b>kinh độ trước</b>: {@code (:lng, :lat)}.</li>
 *   <li>Tên hiển thị lấy theo {@code :locale}, thiếu thì lùi về bản tiếng Việt — không bao giờ
 *       trả {@code null} (FR-I18N-03).</li>
 *   <li>Bộ lọc tuỳ chọn nhận <b>chuỗi rỗng</b> nghĩa là "không lọc", không nhận {@code null}:
 *       Hibernate bind {@code null} không rõ kiểu và Postgres báo lỗi kiểu dữ liệu.</li>
 * </ul>
 */
public interface PlaceRepository extends JpaRepository<Place, UUID> {

    /** Cột chung của mọi truy vấn trả {@link PlaceSummaryProjection}, trừ {@code distanceMeters}. */
    String SUMMARY_COLUMNS = """
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
                   (SELECT COALESCE(m.display_key, m.storage_key) FROM place_media m
                     WHERE m.place_id = p.id AND m.is_cover
                     ORDER BY m.display_order LIMIT 1)         AS coverKey,
            """;

    String TRANSLATION_JOINS = """
            LEFT JOIN place_translations t   ON t.place_id = p.id   AND t.locale = :locale
            LEFT JOIN place_translations tvi ON tvi.place_id = p.id AND tvi.locale = 'vi'
            """;

    /** API công khai chỉ thấy PUBLISHED và TEMPORARILY_CLOSED (FR-PLACE-09). */
    String PUBLICLY_VISIBLE = """
            p.deleted_at IS NULL
              AND p.status IN ('PUBLISHED', 'TEMPORARILY_CLOSED')
            """;

    /**
     * Trang chi tiết (và review của quán) mở được cả quán đã đóng cửa vĩnh viễn — người dùng vẫn
     * vào được từ danh sách yêu thích/lịch sử và thấy nhãn "đã đóng cửa". Chỉ ẩn bản nháp và quán đã xoá.
     */
    String DETAIL_VISIBLE = """
            p.deleted_at IS NULL
              AND p.status <> 'DRAFT'
            """;

    String ORIGIN = "CAST(ST_SetSRID(ST_MakePoint(:lng, :lat), 4326) AS geography)";

    /**
     * Tìm không dấu, không phân biệt hoa thường (FR-PLACE-04). Biểu thức phải viết ĐÚNG như
     * trong định nghĩa index ({@code idx_place_translations_name}, {@code idx_place_translations_description})
     * thì mới trúng index GIN. Tìm trên mọi locale: gõ "pho" hay "Phở" đều ra cùng quán.
     */
    String NAME_MATCH = """
            SELECT x.place_id FROM place_translations x
            WHERE to_tsvector('simple', f_unaccent(x.name)) @@ plainto_tsquery('simple', f_unaccent(:query))
            """;

    String DESCRIPTION_MATCH = """
            SELECT x.place_id FROM place_translations x
            WHERE to_tsvector('simple', f_unaccent(x.description)) @@ plainto_tsquery('simple', f_unaccent(:query))
            """;

    /** Lọc loại và danh mục (thuộc <b>ít nhất một</b> danh mục), dùng chung cho tìm từ khoá và quanh đây. */
    String TYPE_AND_CATEGORY_FILTER = """
              AND (:placeType = '' OR p.place_type = :placeType)
              AND (:categorySlugs = '' OR p.id IN (
                    SELECT pc.place_id FROM place_categories pc
                    JOIN categories c ON c.id = pc.category_id
                    WHERE c.slug = ANY (string_to_array(:categorySlugs, ','))))
            """;

    String SEARCH_FILTER = PUBLICLY_VISIBLE + """
              AND (:query = '' OR p.id IN (""" + NAME_MATCH + ") OR p.id IN (" + DESCRIPTION_MATCH + """
            ))
            """ + TYPE_AND_CATEGORY_FILTER;

    /**
     * Bộ lọc của {@code GET /places/nearby}. {@code :minRating = 0} nghĩa là không lọc
     * (có lọc thì quán chưa có đánh giá bị loại). {@code :openNow}: chỉ quán {@code PUBLISHED}
     * có một khoảng giờ mở chứa {@code :nowTime} — khoảng có {@code open_time >= close_time}
     * là qua nửa đêm, nên xét cả khoảng của hôm trước ({@code :prevDow}).
     * Luật này phải khớp {@link OpeningHours#isOpen}.
     */
    String NEARBY_FILTER = PUBLICLY_VISIBLE
            + "AND ST_DWithin(p.location, " + ORIGIN + ", :radiusMeters)\n"
            + TYPE_AND_CATEGORY_FILTER + """
              AND (:minRating = 0 OR p.average_rating >= :minRating)
              AND (:openNow = FALSE OR (p.status = 'PUBLISHED' AND EXISTS (
                    SELECT 1 FROM opening_hours oh
                    WHERE oh.place_id = p.id AND NOT oh.is_closed_all_day AND (
                         (oh.day_of_week = :dow AND oh.open_time <= CAST(:nowTime AS time)
                            AND (oh.close_time > CAST(:nowTime AS time) OR oh.open_time >= oh.close_time))
                      OR (oh.day_of_week = :prevDow AND oh.open_time >= oh.close_time
                            AND oh.close_time > CAST(:nowTime AS time))))))
            """;

    Optional<Place> findByIdAndDeletedAtIsNull(UUID id);

    Optional<Place> findBySlugAndDeletedAtIsNull(String slug);

    /**
     * Tìm địa điểm quanh một toạ độ, sắp xếp theo khoảng cách tăng dần.
     *
     * <p>Đừng sửa nếu chưa đọc docs/04-data/geo-model.md:
     * {@code ST_DWithin} dùng được index GiST, còn {@code ST_Distance(...) < :radius} trong
     * {@code WHERE} thì <b>không</b> — kết quả vẫn đúng nhưng chậm gấp hàng chục lần.
     * Toán tử KNN {@code <->} sắp xếp theo khoảng cách và cũng dùng index.
     */
    @Query(value = SUMMARY_COLUMNS
            + "ST_Distance(p.location, " + ORIGIN + ") AS distanceMeters\n"
            + "FROM places p\n" + TRANSLATION_JOINS
            + "WHERE " + NEARBY_FILTER
            + "ORDER BY p.location <-> " + ORIGIN,
            countQuery = "SELECT count(*) FROM places p WHERE " + NEARBY_FILTER,
            nativeQuery = true)
    Page<PlaceSummaryProjection> findNearby(@Param("lat") double latitude,
                                            @Param("lng") double longitude,
                                            @Param("radiusMeters") int radiusMeters,
                                            @Param("placeType") String placeType,
                                            @Param("categorySlugs") String categorySlugs,
                                            @Param("minRating") double minRating,
                                            @Param("openNow") boolean openNow,
                                            @Param("nowTime") String nowTime,
                                            @Param("dow") int dayOfWeek,
                                            @Param("prevDow") int previousDayOfWeek,
                                            @Param("locale") String locale,
                                            Pageable pageable);

    /** Một địa điểm công khai kèm các cột riêng của trang chi tiết. */
    @Query(value = SUMMARY_COLUMNS + """
                   CAST(NULL AS double precision)              AS distanceMeters,
                   COALESCE(t.description, tvi.description)    AS description,
                   p.phone                                     AS phone,
                   p.distinct_visitor_count                    AS distinctVisitorCount,
                   p.created_at                                AS createdAt
            FROM places p
            """ + TRANSLATION_JOINS + "WHERE p.id = :id AND " + DETAIL_VISIBLE,
            nativeQuery = true)
    Optional<PlaceDetailProjection> findPublicDetail(@Param("id") UUID id, @Param("locale") String locale);

    /**
     * Tìm theo từ khoá và bộ lọc.
     *
     * <p>{@code sort = RELEVANCE}: khớp tên trước, rồi khớp mô tả, rồi theo tên.
     * {@code sort = POPULAR}: điểm trung bình giảm dần (chưa có đánh giá xếp cuối), rồi số đánh giá.
     */
    @Query(value = SUMMARY_COLUMNS + "CAST(NULL AS double precision) AS distanceMeters\n"
            + "FROM places p\n" + TRANSLATION_JOINS
            + "WHERE " + SEARCH_FILTER
            + """
            ORDER BY CASE WHEN :sort = 'POPULAR' THEN p.average_rating END DESC NULLS LAST,
                     CASE WHEN :sort = 'POPULAR' THEN p.review_count END DESC,
                     CASE WHEN :query <> '' AND p.id IN (""" + NAME_MATCH + """
            ) THEN 0 ELSE 1 END,
                     COALESCE(t.name, tvi.name), p.id
            """,
            countQuery = "SELECT count(*) FROM places p WHERE " + SEARCH_FILTER,
            nativeQuery = true)
    Page<PlaceSummaryProjection> search(@Param("query") String query,
                                        @Param("placeType") String placeType,
                                        @Param("categorySlugs") String categorySlugs,
                                        @Param("sort") String sort,
                                        @Param("locale") String locale,
                                        Pageable pageable);

    /**
     * Địa điểm người dùng đã yêu thích. Không lọc theo trạng thái công khai: quán đã đóng cửa
     * vẫn nằm trong danh sách kèm nhãn {@code status} — chỉ bỏ bản nháp và bản đã xoá.
     *
     * <p>{@code :byDistance = false} thì {@code :lat}/{@code :lng} bị bỏ qua và
     * {@code distanceMeters} là {@code null}.
     */
    @Query(value = SUMMARY_COLUMNS
            + "CASE WHEN :byDistance THEN ST_Distance(p.location, " + ORIGIN + ") END AS distanceMeters\n"
            + "FROM favorites f JOIN places p ON p.id = f.place_id\n" + TRANSLATION_JOINS
            + """
            WHERE f.user_id = :userId AND p.deleted_at IS NULL AND p.status <> 'DRAFT'
            ORDER BY CASE WHEN :byDistance THEN p.location <->""" + " " + ORIGIN + """
             END, f.created_at DESC, p.id
            """,
            countQuery = """
            SELECT count(*) FROM favorites f JOIN places p ON p.id = f.place_id
            WHERE f.user_id = :userId AND p.deleted_at IS NULL AND p.status <> 'DRAFT'
            """,
            nativeQuery = true)
    Page<PlaceSummaryProjection> findFavorites(@Param("userId") UUID userId,
                                               @Param("byDistance") boolean byDistance,
                                               @Param("lat") double latitude,
                                               @Param("lng") double longitude,
                                               @Param("locale") String locale,
                                               Pageable pageable);
}
