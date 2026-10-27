package com.foodmap.review;

import com.foodmap.common.PageResponse;
import com.foodmap.common.exception.NotFoundException;
import com.foodmap.media.MediaUrls;
import com.foodmap.media.dto.MediaDto;
import com.foodmap.review.dto.ReviewDto;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Đọc đánh giá. Chỉ đọc nên truy vấn thẳng bằng SQL; entity sẽ có khi làm luồng gửi/kiểm duyệt.
 */
@Service
public class ReviewService {

    private static final String SELECT = """
            SELECT r.id, r.place_id, r.rating, r.content, r.locale, r.status, r.moderation_note,
                   r.created_at, r.updated_at,
                   u.id AS author_id, u.display_name, u.avatar_url,
                   p.place_type, COALESCE(t.name, tvi.name) AS place_name,
                   (SELECT COALESCE(m.display_key, m.storage_key) FROM place_media m
                     WHERE m.place_id = p.id AND m.is_cover
                     ORDER BY m.display_order LIMIT 1) AS cover_key
            FROM reviews r
            JOIN users u  ON u.id = r.user_id
            JOIN places p ON p.id = r.place_id
            LEFT JOIN place_translations t   ON t.place_id = p.id   AND t.locale = :locale
            LEFT JOIN place_translations tvi ON tvi.place_id = p.id AND tvi.locale = 'vi'
            """;

    /** Chỉ đánh giá đã duyệt, của địa điểm đang hiển thị công khai (FR-PLACE-09). */
    private static final String FEED_FILTER = """
            WHERE r.status = 'APPROVED' AND r.deleted_at IS NULL
              AND p.deleted_at IS NULL AND p.status IN ('PUBLISHED', 'TEMPORARILY_CLOSED')
            """;

    private static final String OWN_FILTER = "WHERE r.user_id = :userId AND r.deleted_at IS NULL\n";

    private static final String NEWEST_FIRST = "ORDER BY r.created_at DESC, r.id DESC LIMIT :limit OFFSET :offset";

    /**
     * Đánh giá của một địa điểm: bản đã duyệt, cộng mọi bản của chính người gọi (luôn thấy kèm
     * nhãn trạng thái). Khách thì {@code :viewerId} là UUID 0 — không trùng ai.
     */
    private static final String PLACE_FILTER = """
            WHERE r.place_id = :placeId AND r.deleted_at IS NULL
              AND (r.status = 'APPROVED' OR r.user_id = :viewerId)
            """;

    private static final UUID NOBODY = new UUID(0, 0);

    /** Thứ tự của {@code GET /places/{placeId}/reviews}. Khớp tham số {@code sort} trong openapi.yaml. */
    public enum PlaceReviewSort {
        NEWEST("r.created_at DESC"),
        HIGHEST_RATING("r.rating DESC, r.created_at DESC"),
        LOWEST_RATING("r.rating ASC, r.created_at DESC");

        private final String orderBy;

        PlaceReviewSort(String orderBy) {
            this.orderBy = orderBy;
        }
    }

    private final JdbcClient jdbc;
    private final MediaUrls mediaUrls;

    public ReviewService(JdbcClient jdbc, MediaUrls mediaUrls) {
        this.jdbc = jdbc;
        this.mediaUrls = mediaUrls;
    }

    /** Bảng tin, mới nhất trước. Không trả {@code moderationNote} — chỉ tác giả và moderator được thấy. */
    @Transactional(readOnly = true)
    public PageResponse<ReviewDto> listFeed(Pageable pageable) {
        List<ReviewDto> rows = jdbc.sql(SELECT + FEED_FILTER + NEWEST_FIRST)
                .param("locale", locale())
                .param("limit", pageable.getPageSize())
                .param("offset", pageable.getOffset())
                .query((rs, n) -> map(rs, false))
                .list();
        long total = jdbc.sql("SELECT count(*) FROM reviews r JOIN places p ON p.id = r.place_id\n" + FEED_FILTER)
                .query(Long.class).single();
        return page(rows, pageable, total);
    }

    /**
     * Đánh giá của một địa điểm (kể cả đã đóng cửa vĩnh viễn, như trang chi tiết); không có thì 404 {@code PLACE_NOT_FOUND}.
     * {@code moderationNote} chỉ điền cho review của chính người gọi; {@code place} luôn null (theo spec).
     *
     * @param viewerId người gọi, {@code null} khi chưa đăng nhập
     */
    @Transactional(readOnly = true)
    public PageResponse<ReviewDto> listForPlace(UUID placeId, UUID viewerId, PlaceReviewSort sort, Pageable pageable) {
        boolean visible = jdbc.sql("""
                        SELECT EXISTS (SELECT 1 FROM places WHERE id = :placeId AND deleted_at IS NULL
                                       AND status <> 'DRAFT')
                        """)
                .param("placeId", placeId)
                .query(Boolean.class).single();
        if (!visible) {
            throw new NotFoundException("PLACE_NOT_FOUND", "place.error.not_found");
        }
        UUID viewer = viewerId == null ? NOBODY : viewerId;
        List<ReviewDto> rows = jdbc.sql(SELECT + PLACE_FILTER
                        + "ORDER BY " + sort.orderBy + ", r.id DESC LIMIT :limit OFFSET :offset")
                .param("locale", locale())
                .param("placeId", placeId)
                .param("viewerId", viewer)
                .param("limit", pageable.getPageSize())
                .param("offset", pageable.getOffset())
                .query((rs, n) -> {
                    ReviewDto r = map(rs, viewer.equals(rs.getObject("author_id", UUID.class)));
                    return new ReviewDto(r.id(), r.placeId(), r.author(), r.rating(), r.content(), r.locale(),
                            r.status(), r.moderationNote(), r.media(), null, r.createdAt(), r.updatedAt());
                })
                .list();
        long total = jdbc.sql("SELECT count(*) FROM reviews r " + PLACE_FILTER)
                .param("placeId", placeId)
                .param("viewerId", viewer)
                .query(Long.class).single();
        return page(rows, pageable, total);
    }

    /** Đánh giá của chính người gọi, mọi trạng thái, kèm lý do kiểm duyệt. */
    @Transactional(readOnly = true)
    public PageResponse<ReviewDto> listOwn(UUID userId, Pageable pageable) {
        List<ReviewDto> rows = jdbc.sql(SELECT + OWN_FILTER + NEWEST_FIRST)
                .param("locale", locale())
                .param("userId", userId)
                .param("limit", pageable.getPageSize())
                .param("offset", pageable.getOffset())
                .query((rs, n) -> map(rs, true))
                .list();
        long total = jdbc.sql("SELECT count(*) FROM reviews r " + OWN_FILTER)
                .param("userId", userId)
                .query(Long.class).single();
        return page(rows, pageable, total);
    }

    /** Gắn media cho cả trang bằng một truy vấn. Chỉ media đã xác nhận upload xong. */
    private PageResponse<ReviewDto> page(List<ReviewDto> rows, Pageable pageable, long total) {
        Map<UUID, List<MediaDto>> media = rows.isEmpty() ? Map.of() : jdbc.sql("""
                        SELECT id, review_id, media_type, storage_key, display_key, thumbnail_key,
                               width, height, duration_seconds
                        FROM review_media
                        WHERE review_id IN (:ids) AND confirmed_at IS NOT NULL
                        ORDER BY display_order, created_at
                        """)
                .param("ids", rows.stream().map(ReviewDto::id).toList())
                .query((rs, n) -> Map.entry(rs.getObject("review_id", UUID.class), new MediaDto(
                        rs.getObject("id", UUID.class),
                        rs.getString("media_type"),
                        mediaUrls.of(firstNonNull(rs.getString("display_key"), rs.getString("storage_key"))),
                        mediaUrls.of(rs.getString("thumbnail_key")),
                        rs.getObject("width", Integer.class),
                        rs.getObject("height", Integer.class),
                        rs.getObject("duration_seconds", Integer.class))))
                .list()
                .stream()
                .collect(Collectors.groupingBy(Map.Entry::getKey,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())));

        List<ReviewDto> content = rows.stream()
                .map(r -> r.withMedia(media.getOrDefault(r.id(), List.of())))
                .toList();
        return PageResponse.from(new PageImpl<>(content, pageable, total));
    }

    private ReviewDto map(ResultSet rs, boolean withModerationNote) throws SQLException {
        UUID placeId = rs.getObject("place_id", UUID.class);
        return new ReviewDto(
                rs.getObject("id", UUID.class),
                placeId,
                new ReviewDto.Author(
                        rs.getObject("author_id", UUID.class),
                        rs.getString("display_name"),
                        rs.getString("avatar_url")),
                rs.getInt("rating"),
                rs.getString("content"),
                rs.getString("locale"),
                rs.getString("status"),
                withModerationNote ? rs.getString("moderation_note") : null,
                List.of(),
                new ReviewDto.Place(
                        placeId,
                        rs.getString("place_name"),
                        rs.getString("place_type"),
                        mediaUrls.of(rs.getString("cover_key"))),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant());
    }

    private static String firstNonNull(String a, String b) {
        return a != null ? a : b;
    }

    private static String locale() {
        return LocaleContextHolder.getLocale().getLanguage();
    }
}
