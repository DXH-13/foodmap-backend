package com.foodmap.visit;

import com.foodmap.common.PageResponse;
import com.foodmap.common.exception.ConflictException;
import com.foodmap.common.exception.NotFoundException;
import com.foodmap.config.props.FoodmapProperties;
import com.foodmap.visit.dto.VisitDto;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

/** Ghi nhận và đọc lượt đến (FR-VISIT-02). Truy vấn thẳng bằng SQL, chưa có entity. */
@Service
public class VisitService {

    /** "Một lượt mỗi ngày" tính theo ngày ở Việt Nam, không theo giờ máy chủ hay UTC. */
    private static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");

    private final JdbcClient jdbc;
    private final int maxDistanceMeters;

    public VisitService(JdbcClient jdbc, FoodmapProperties properties) {
        this.jdbc = jdbc;
        this.maxDistanceMeters = properties.geo().visitMaxDistanceMeters();
    }

    /**
     * Ghi nhận một lượt đến.
     * <ul>
     *   <li>Địa điểm không công khai/không tồn tại → 404 {@code PLACE_NOT_FOUND}.</li>
     *   <li>Cách quá {@code foodmap.geo.visit-max-distance-meters} (200m) → 409 {@code VISIT_TOO_FAR}.</li>
     *   <li>Đã có lượt trong ngày (giờ Việt Nam) → 409 {@code VISIT_ALREADY_TODAY}; index
     *       {@code uq_visits_user_place_day} chặn cả khi hai request chạy song song.</li>
     * </ul>
     * Thành công thì tăng {@code visit_count}, và {@code distinct_visitor_count} nếu là lần đầu người này đến.
     */
    @Transactional
    public VisitDto record(UUID userId, UUID placeId, double latitude, double longitude) {
        Double distance = jdbc.sql("""
                        SELECT ST_Distance(location, CAST(ST_SetSRID(ST_MakePoint(:lng, :lat), 4326) AS geography))
                        FROM places
                        WHERE id = :placeId AND deleted_at IS NULL AND status IN ('PUBLISHED', 'TEMPORARILY_CLOSED')
                        """)
                .param("placeId", placeId)
                .param("lat", latitude)
                .param("lng", longitude)
                .query(Double.class).optional()
                .orElseThrow(() -> new NotFoundException("PLACE_NOT_FOUND", "place.error.not_found"));

        int meters = (int) Math.round(distance);
        if (meters > maxDistanceMeters) {
            throw new ConflictException("VISIT_TOO_FAR", "visit.error.too_far", String.valueOf(meters));
        }

        LocalDate today = LocalDate.now(VIETNAM);
        UUID visitId = jdbc.sql("""
                        INSERT INTO visits (user_id, place_id, visit_date, recorded_location, distance_meters)
                        VALUES (:userId, :placeId, :visitDate,
                                CAST(ST_SetSRID(ST_MakePoint(:lng, :lat), 4326) AS geography), :meters)
                        ON CONFLICT (user_id, place_id, visit_date) DO NOTHING
                        RETURNING id
                        """)
                .param("userId", userId)
                .param("placeId", placeId)
                .param("visitDate", today)
                .param("lat", latitude)
                .param("lng", longitude)
                .param("meters", meters)
                .query(UUID.class).optional()
                .orElseThrow(() -> new ConflictException("VISIT_ALREADY_TODAY", "visit.error.already_today"));

        jdbc.sql("""
                        UPDATE places SET visit_count = visit_count + 1,
                               distinct_visitor_count = distinct_visitor_count
                                   + CASE WHEN (SELECT count(*) FROM visits
                                                WHERE user_id = :userId AND place_id = :placeId) = 1 THEN 1 ELSE 0 END
                        WHERE id = :placeId
                        """)
                .param("userId", userId)
                .param("placeId", placeId)
                .update();

        return jdbc.sql("""
                        SELECT v.id, v.place_id, COALESCE(t.name, tvi.name) AS place_name,
                               v.visit_date, v.distance_meters, v.created_at
                        FROM visits v
                        LEFT JOIN place_translations t   ON t.place_id = v.place_id   AND t.locale = :locale
                        LEFT JOIN place_translations tvi ON tvi.place_id = v.place_id AND tvi.locale = 'vi'
                        WHERE v.id = :id
                        """)
                .param("locale", LocaleContextHolder.getLocale().getLanguage())
                .param("id", visitId)
                .query((rs, n) -> new VisitDto(
                        rs.getObject("id", UUID.class),
                        rs.getObject("place_id", UUID.class),
                        rs.getString("place_name"),
                        rs.getObject("visit_date", LocalDate.class),
                        rs.getInt("distance_meters"),
                        rs.getTimestamp("created_at").toInstant()))
                .single();
    }

    @Transactional(readOnly = true)
    public PageResponse<VisitDto> listOwn(UUID userId, Pageable pageable) {
        List<VisitDto> rows = jdbc.sql("""
                        SELECT v.id, v.place_id, COALESCE(t.name, tvi.name) AS place_name,
                               v.visit_date, v.distance_meters, v.created_at
                        FROM visits v
                        LEFT JOIN place_translations t   ON t.place_id = v.place_id   AND t.locale = :locale
                        LEFT JOIN place_translations tvi ON tvi.place_id = v.place_id AND tvi.locale = 'vi'
                        WHERE v.user_id = :userId
                        ORDER BY v.created_at DESC, v.id DESC
                        LIMIT :limit OFFSET :offset
                        """)
                .param("locale", LocaleContextHolder.getLocale().getLanguage())
                .param("userId", userId)
                .param("limit", pageable.getPageSize())
                .param("offset", pageable.getOffset())
                .query((rs, n) -> new VisitDto(
                        rs.getObject("id", UUID.class),
                        rs.getObject("place_id", UUID.class),
                        rs.getString("place_name"),
                        rs.getObject("visit_date", LocalDate.class),
                        rs.getInt("distance_meters"),
                        rs.getTimestamp("created_at").toInstant()))
                .list();
        long total = jdbc.sql("SELECT count(*) FROM visits WHERE user_id = :userId")
                .param("userId", userId)
                .query(Long.class).single();
        return PageResponse.from(new PageImpl<>(rows, pageable, total));
    }

    /** Số lượt đến của một người tại một địa điểm — trường {@code ownVisitCount} của trang chi tiết. */
    @Transactional(readOnly = true)
    public int countOwnAtPlace(UUID userId, UUID placeId) {
        return jdbc.sql("SELECT count(*) FROM visits WHERE user_id = :userId AND place_id = :placeId")
                .param("userId", userId)
                .param("placeId", placeId)
                .query(Integer.class).single();
    }
}
