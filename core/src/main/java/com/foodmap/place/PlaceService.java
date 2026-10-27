package com.foodmap.place;

import com.foodmap.category.CategoryService;
import com.foodmap.category.dto.CategoryDto;
import com.foodmap.common.PageResponse;
import com.foodmap.common.exception.BadRequestException;
import com.foodmap.common.exception.NotFoundException;
import com.foodmap.config.props.FoodmapProperties;
import com.foodmap.media.MediaUrls;
import com.foodmap.media.dto.MediaDto;
import com.foodmap.place.dto.CoordinatesDto;
import com.foodmap.place.dto.OpeningHourRangeDto;
import com.foodmap.place.dto.PlaceDetailDto;
import com.foodmap.place.dto.PlaceExternalLinkDto;
import com.foodmap.place.dto.PlaceSummaryDto;
import com.foodmap.visit.VisitService;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PlaceService {

    /** Thứ tự kết quả của {@code GET /places}. Khớp tham số {@code sort} trong openapi.yaml. */
    public enum SearchSort { RELEVANCE, POPULAR }

    /** Thứ tự kết quả của {@code GET /me/favorites}. */
    public enum FavoriteSort { RECENTLY_ADDED, DISTANCE }

    /**
     * Bộ lọc tuỳ chọn của {@code GET /places/nearby}; {@code null}/rỗng nghĩa là không lọc.
     * {@code openNow = true} chỉ lấy quán đang mở theo giờ {@code Asia/Ho_Chi_Minh}.
     */
    public record NearbyFilter(PlaceType placeType, List<String> categorySlugs, Double minRating, boolean openNow) {
    }

    /** Giờ mở cửa tính theo giờ Việt Nam, không theo giờ máy chủ. */
    private static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    private final PlaceRepository placeRepository;
    private final CategoryService categoryService;
    private final MediaUrls mediaUrls;
    private final FoodmapProperties.Geo geo;
    private final VisitService visitService;
    private final JdbcClient jdbc;

    public PlaceService(PlaceRepository placeRepository,
                        CategoryService categoryService,
                        MediaUrls mediaUrls,
                        FoodmapProperties properties,
                        VisitService visitService,
                        JdbcClient jdbc) {
        this.placeRepository = placeRepository;
        this.categoryService = categoryService;
        this.mediaUrls = mediaUrls;
        this.geo = properties.geo();
        this.visitService = visitService;
        this.jdbc = jdbc;
    }

    /**
     * Tìm địa điểm quanh một toạ độ.
     *
     * <p>Bán kính ngoài khoảng cho phép trả về lỗi thay vì bị cắt bớt âm thầm
     * (FR-PLACE-02): người gọi cần biết yêu cầu của họ không được thực hiện như đã yêu cầu.
     */
    @Transactional(readOnly = true)
    public PageResponse<PlaceSummaryDto> searchNearby(double latitude,
                                                      double longitude,
                                                      Integer radiusMeters,
                                                      NearbyFilter filter,
                                                      Pageable pageable) {

        int radius = radiusMeters == null ? geo.defaultRadiusMeters() : radiusMeters;

        if (radius < geo.minRadiusMeters() || radius > geo.maxRadiusMeters()) {
            throw new BadRequestException(
                    "RADIUS_OUT_OF_RANGE",
                    "place.error.radius_out_of_range",
                    geo.minRadiusMeters(), geo.maxRadiusMeters());
        }
        validateCoordinates(latitude, longitude);

        LocalDateTime now = LocalDateTime.now(VIETNAM);
        return toSummaries(placeRepository.findNearby(
                latitude, longitude, radius,
                filter.placeType() == null ? "" : filter.placeType().name(),
                filter.categorySlugs() == null ? "" : String.join(",", filter.categorySlugs()),
                filter.minRating() == null ? 0 : filter.minRating(),
                filter.openNow(),
                now.toLocalTime().withNano(0).toString(),
                now.getDayOfWeek().getValue(),
                now.minusDays(1).getDayOfWeek().getValue(),
                locale(),
                pageable));
    }

    /**
     * Chi tiết một địa điểm công khai (PLACE-01). Bản nháp, đã đóng cửa vĩnh viễn hoặc đã xoá
     * trả 404 {@code PLACE_NOT_FOUND}, giống hệt id không tồn tại.
     *
     * @param userId người gọi; {@code null} khi chưa đăng nhập, khi đó {@code isFavorited}
     *               và {@code ownVisitCount} là {@code null}
     */
    @Transactional(readOnly = true)
    public PlaceDetailDto getDetail(UUID placeId, UUID userId) {
        PlaceDetailProjection row = placeRepository.findPublicDetail(placeId, locale())
                .orElseThrow(() -> new NotFoundException("PLACE_NOT_FOUND", "place.error.not_found"));

        List<OpeningHours.Range> hours = jdbc.sql("""
                        SELECT day_of_week, open_time, close_time, is_closed_all_day FROM opening_hours
                        WHERE place_id = :placeId ORDER BY day_of_week, open_time NULLS FIRST
                        """)
                .param("placeId", placeId)
                .query((rs, n) -> new OpeningHours.Range(
                        rs.getInt("day_of_week"),
                        rs.getObject("open_time", LocalTime.class),
                        rs.getObject("close_time", LocalTime.class),
                        rs.getBoolean("is_closed_all_day")))
                .list();

        // place_media chỉ chứa ảnh — bảng không có cột loại media
        List<MediaDto> images = jdbc.sql("""
                        SELECT id, COALESCE(display_key, storage_key) AS url_key, thumbnail_key, width, height
                        FROM place_media WHERE place_id = :placeId
                        ORDER BY is_cover DESC, display_order, created_at
                        """)
                .param("placeId", placeId)
                .query((rs, n) -> new MediaDto(
                        rs.getObject("id", UUID.class),
                        "IMAGE",
                        mediaUrls.of(rs.getString("url_key")),
                        mediaUrls.of(rs.getString("thumbnail_key")),
                        rs.getObject("width", Integer.class),
                        rs.getObject("height", Integer.class),
                        null))
                .list();

        List<PlaceExternalLinkDto> externalLinks = jdbc.sql("""
                        SELECT id, platform, url, title FROM place_external_links
                        WHERE place_id = :placeId ORDER BY display_order, created_at
                        """)
                .param("placeId", placeId)
                .query((rs, n) -> new PlaceExternalLinkDto(
                        rs.getObject("id", UUID.class),
                        rs.getString("platform"),
                        rs.getString("url"),
                        rs.getString("title")))
                .list();

        // Chưa có giờ mở cửa: không biết, trả null chứ không đoán là đóng.
        // Tạm hoặc đã đóng hẳn: đang đóng và không có "mở cửa lúc nào tiếp theo".
        LocalDateTime now = LocalDateTime.now(VIETNAM);
        boolean temporarilyClosed = PlaceStatus.TEMPORARILY_CLOSED.name().equals(row.getStatus())
                || PlaceStatus.PERMANENTLY_CLOSED.name().equals(row.getStatus());
        Boolean openNow = hours.isEmpty() ? null : !temporarilyClosed && OpeningHours.isOpen(hours, now);
        var nextOpenAt = Boolean.FALSE.equals(openNow) && !temporarilyClosed
                ? OpeningHours.nextOpen(hours, now).map(at -> at.atZone(VIETNAM).toInstant()).orElse(null)
                : null;

        Boolean favorited = userId == null ? null : jdbc.sql(
                        "SELECT EXISTS (SELECT 1 FROM favorites WHERE user_id = :userId AND place_id = :placeId)")
                .param("userId", userId)
                .param("placeId", placeId)
                .query(Boolean.class).single();

        return new PlaceDetailDto(
                row.getId(),
                row.getSlug(),
                row.getName(),
                row.getPlaceType(),
                new CoordinatesDto(row.getLatitude(), row.getLongitude()),
                row.getAddress(),
                row.getStatus(),
                row.getAverageRating(),
                row.getReviewCount(),
                row.getVisitCount(),
                mediaUrls.of(row.getCoverKey()),
                null,
                openNow,
                categoryService.findByPlaceIds(List.of(placeId)).getOrDefault(placeId, List.of()),
                row.getDescription(),
                row.getPhone(),
                row.getDistinctVisitorCount(),
                nextOpenAt,
                hours.stream().map(PlaceService::toDto).toList(),
                images,
                externalLinks,
                favorited,
                userId == null ? null : visitService.countOwnAtPlace(userId, placeId),
                row.getCreatedAt());
    }

    private static OpeningHourRangeDto toDto(OpeningHours.Range r) {
        return new OpeningHourRangeDto(
                r.dayOfWeek(),
                r.open() == null ? null : r.open().format(HH_MM),
                r.close() == null ? null : r.close().format(HH_MM),
                r.closedAllDay());
    }

    /**
     * Tìm theo từ khoá (không dấu, không phân biệt hoa thường) trên tên và mô tả mọi ngôn ngữ,
     * kèm lọc theo loại và danh mục (lấy địa điểm thuộc <b>ít nhất một</b> danh mục).
     */
    @Transactional(readOnly = true)
    public PageResponse<PlaceSummaryDto> search(String query,
                                                PlaceType placeType,
                                                List<String> categorySlugs,
                                                SearchSort sort,
                                                Pageable pageable) {
        return toSummaries(placeRepository.search(
                query == null ? "" : query.strip(),
                placeType == null ? "" : placeType.name(),
                categorySlugs == null ? "" : String.join(",", categorySlugs),
                sort.name(),
                locale(),
                pageable));
    }

    /** {@code DISTANCE} bắt buộc có toạ độ — thiếu thì 400, không âm thầm đổi sang thứ tự khác. */
    @Transactional(readOnly = true)
    public PageResponse<PlaceSummaryDto> listFavorites(UUID userId,
                                                       FavoriteSort sort,
                                                       Double latitude,
                                                       Double longitude,
                                                       Pageable pageable) {
        boolean byDistance = sort == FavoriteSort.DISTANCE;
        if (byDistance) {
            if (latitude == null || longitude == null) {
                throw new BadRequestException("COORDINATES_REQUIRED", "favorite.error.coordinates_required");
            }
            validateCoordinates(latitude, longitude);
        }
        return toSummaries(placeRepository.findFavorites(
                userId,
                byDistance,
                byDistance ? latitude : 0,
                byDistance ? longitude : 0,
                locale(),
                pageable));
    }

    /** Gắn danh mục bằng một truy vấn cho cả trang — không truy vấn từng địa điểm (N+1). */
    private PageResponse<PlaceSummaryDto> toSummaries(Page<PlaceSummaryProjection> page) {
        Map<UUID, List<CategoryDto>> categories = categoryService.findByPlaceIds(
                page.getContent().stream().map(PlaceSummaryProjection::getId).toList());
        return PageResponse.from(page, row -> PlaceSummaryDto.from(
                row,
                mediaUrls.of(row.getCoverKey()),
                categories.getOrDefault(row.getId(), List.of())));
    }

    private static void validateCoordinates(double latitude, double longitude) {
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new BadRequestException("INVALID_COORDINATES", "place.error.invalid_coordinates");
        }
    }

    private static String locale() {
        return LocaleContextHolder.getLocale().getLanguage();
    }
}
