package com.foodmap.place.application;

import com.foodmap.common.BadRequestException;
import com.foodmap.common.NotFoundException;
import com.foodmap.common.PageResponse;
import com.foodmap.place.PlaceGeoProperties;
import com.foodmap.place.api.PlaceApi;
import com.foodmap.place.api.PlaceDetails;
import com.foodmap.place.api.PlaceSummary;
import com.foodmap.place.domain.Place;
import com.foodmap.place.infrastructure.PlaceRepository;
import com.foodmap.place.web.dto.PlaceSummaryDto;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class PlaceService implements PlaceApi {

    private final PlaceRepository placeRepository;
    private final PlaceGeoProperties geo;

    public PlaceService(PlaceRepository placeRepository, PlaceGeoProperties geo) {
        this.placeRepository = placeRepository;
        this.geo = geo;
    }

    /**
     * Địa điểm tồn tại và chưa bị xoá mềm. Dùng cho module khác (ví dụ review ở
     * Bước 3) kiểm tra địa điểm trước khi gắn dữ liệu vào — không kiểm tra trạng thái
     * hiển thị công khai, vì địa điểm nháp vẫn có thể nhận đánh giá nội bộ.
     */
    @Transactional(readOnly = true)
    @Override
    public boolean existsById(java.util.UUID placeId) {
        return placeRepository.findByIdAndDeletedAtIsNull(placeId).isPresent();
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<UUID> findCreatorById(UUID placeId) {
        return placeRepository.findByIdAndDeletedAtIsNull(placeId)
                .map(Place::getCreatedBy)
                .filter(Objects::nonNull);
    }

    @Transactional(readOnly = true)
    @Override
    public PlaceDetails getPlace(UUID placeId) {
        String locale = LocaleContextHolder.getLocale().getLanguage();
        return placeRepository.findDetailsById(placeId, locale)
                .map(row -> new PlaceDetails(
                        row.getId(), row.getName(), row.getLatitude(), row.getLongitude(), row.getStatus()))
                .orElseThrow(() -> new NotFoundException("PLACE_NOT_FOUND", "place.error.not_found"));
    }

    @Transactional(readOnly = true)
    @Override
    public double distanceMetersTo(UUID placeId, double latitude, double longitude) {
        return placeRepository.distanceMetersTo(placeId, latitude, longitude)
                .orElseThrow(() -> new NotFoundException("PLACE_NOT_FOUND", "place.error.not_found"));
    }

    @Transactional(readOnly = true)
    @Override
    public List<PlaceSummary> getSummaries(List<UUID> placeIds, String locale) {
        if (placeIds == null || placeIds.isEmpty()) {
            return List.of();
        }
        return placeRepository.findSummariesByIds(placeIds, locale).stream()
                .map(row -> new PlaceSummary(
                        row.getId(), row.getSlug(), row.getName(), row.getPlaceType(),
                        row.getLatitude(), row.getLongitude(), row.getAddress(), row.getStatus(),
                        row.getAverageRating(), row.getReviewCount(), row.getVisitCount()))
                .toList();
    }

    /**
     * Áp dụng số liệu đánh giá do module {@code review} tính từ dữ liệu của nó.
     * Chỉ cập nhật 2 cột dẫn xuất, không đụng tới phần còn lại của địa điểm.
     */
    @Transactional
    @Override
    public void updateRatingStats(UUID placeId, BigDecimal averageRating, int reviewCount) {
        Place place = requirePlace(placeId);
        place.setAverageRating(averageRating);
        place.setReviewCount(reviewCount);
        placeRepository.save(place);
    }

    /** Áp dụng số liệu ghé thăm do module {@code visit} tính từ dữ liệu của nó. */
    @Transactional
    @Override
    public void updateVisitStats(UUID placeId, long visitCount, long distinctVisitorCount) {
        Place place = requirePlace(placeId);
        place.setVisitCount(visitCount);
        place.setDistinctVisitorCount(distinctVisitorCount);
        placeRepository.save(place);
    }

    private Place requirePlace(UUID placeId) {
        return placeRepository.findByIdAndDeletedAtIsNull(placeId)
                .orElseThrow(() -> new NotFoundException("PLACE_NOT_FOUND", "place.error.not_found"));
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
                                                      Pageable pageable) {

        int radius = resolveRadius(radiusMeters);
        validateCoordinates(latitude, longitude);

        String locale = LocaleContextHolder.getLocale().getLanguage();

        return PageResponse.from(
                placeRepository.findNearby(latitude, longitude, radius, locale, pageable),
                PlaceSummaryDto::from);
    }

    /**
     * Tìm quanh đây qua {@link PlaceApi}, cho tool {@code search_places} của
     * chatbot ở Bước 5. Cùng luật kiểm tra với endpoint công khai, chỉ khác là
     * trả danh sách rút gọn thay vì trang.
     */
    @Transactional(readOnly = true)
    @Override
    public List<PlaceSummary> searchNearby(double latitude, double longitude,
                                         Integer radiusMeters, int limit) {
        int radius = resolveRadius(radiusMeters);
        validateCoordinates(latitude, longitude);
        String locale = LocaleContextHolder.getLocale().getLanguage();
        return placeRepository.findNearby(latitude, longitude, radius, locale,
                        PageRequest.of(0, clampLimit(limit))).stream()
                .map(this::toSummary)
                .toList();
    }

    /**
     * Tìm theo từ khoá qua {@link PlaceApi}, cho tool {@code search_places} của
     * chatbot ở Bước 5 khi câu hỏi không kèm toạ độ.
     */
    @Transactional(readOnly = true)
    @Override
    public List<PlaceSummary> searchByKeyword(String keyword, int limit) {
        String locale = LocaleContextHolder.getLocale().getLanguage();
        return placeRepository.searchByKeyword(escapeLikeWildcard(keyword), locale,
                        PageRequest.of(0, clampLimit(limit))).stream()
                .map(this::toSummary)
                .toList();
    }

    private PlaceSummary toSummary(com.foodmap.place.domain.PlaceSummaryProjection row) {
        return new PlaceSummary(
                row.getId(), row.getSlug(), row.getName(), row.getPlaceType(),
                row.getLatitude(), row.getLongitude(), row.getAddress(), row.getStatus(),
                row.getAverageRating(), row.getReviewCount(), row.getVisitCount());
    }

    private int resolveRadius(Integer radiusMeters) {
        int radius = radiusMeters == null ? geo.defaultRadiusMeters() : radiusMeters;
        if (radius < geo.minRadiusMeters() || radius > geo.maxRadiusMeters()) {
            throw new BadRequestException(
                    "RADIUS_OUT_OF_RANGE",
                    "place.error.radius_out_of_range",
                    geo.minRadiusMeters(), geo.maxRadiusMeters());
        }
        return radius;
    }

    private static void validateCoordinates(double latitude, double longitude) {
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new BadRequestException("INVALID_COORDINATES", "place.error.invalid_coordinates");
        }
    }

    /** Giới hạn số kết quả của tool nội bộ: tối thiểu 1, tối đa 10. */
    private static int clampLimit(int limit) {
        return Math.clamp(limit, 1, 10);
    }

    /**
     * Escape ký tự đại diện của ILIKE ({@code %}, {@code _}, {@code \}) trong
     * từ khoá người dùng nhập — nếu không, "100%" sẽ khớp với mọi chuỗi bắt đầu
     * bằng "100". Postgres dùng {@code \} làm ký tự escape mặc định của LIKE.
     */
    private static String escapeLikeWildcard(String keyword) {
        return keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
