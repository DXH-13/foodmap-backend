package com.foodmap.visit.application;

import com.foodmap.common.BadRequestException;
import com.foodmap.common.ConflictException;
import com.foodmap.common.GeoUtils;
import com.foodmap.common.PageResponse;
import com.foodmap.place.api.PlaceApi;
import com.foodmap.place.api.PlaceDetails;
import com.foodmap.visit.api.VisitApi;
import com.foodmap.visit.api.VisitCounts;
import com.foodmap.visit.api.VisitRecorded;
import com.foodmap.visit.domain.Visit;
import com.foodmap.visit.infrastructure.VisitRepository;
import com.foodmap.visit.web.dto.RecordVisitRequest;
import com.foodmap.visit.web.dto.VisitDto;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Use case ghé thăm (Phase 3): ghi nhận lượt đến và xem lịch sử.
 *
 * <p>Luật ghi nhận (FR-VISIT-02, theo hợp đồng): vị trí hiện tại phải nằm trong
 * 200m quanh địa điểm, tối đa 1 lượt / người / địa điểm / ngày theo giờ
 * {@code Asia/Ho_Chi_Minh}.
 */
@Service
public class VisitService implements VisitApi {

    /** Múi giờ tính "ngày" cho luật 1 lượt/ngày. */
    static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    /**
     * Bán kính tối đa cho phép ghi nhận một lượt đến. Con số này trùng với
     * {@code foodmap.geo.visit-max-distance-meters} trong {@code application.yml}
     * và với hợp đồng — giữ thành hằng ở đây vì {@code visit} không được đọc
     * cấu hình của module {@code place}; đổi một nơi thì phải đổi cả hai.
     */
    static final int MAX_DISTANCE_METERS = 200;

    private final VisitRepository visits;
    private final PlaceApi places;
    private final ApplicationEventPublisher events;

    public VisitService(VisitRepository visits, PlaceApi places, ApplicationEventPublisher events) {
        this.visits = visits;
        this.places = places;
        this.events = events;
    }

    /** Ghi nhận một lượt đến. Trả 409 khi quá xa hoặc đã ghi nhận trong ngày. */
    @Transactional
    public VisitDto recordVisit(UUID placeId, UUID userId, RecordVisitRequest request) {
        PlaceDetails place = places.getPlace(placeId);
        double latitude = request.coordinates().latitude();
        double longitude = request.coordinates().longitude();
        double distance = places.distanceMetersTo(placeId, latitude, longitude);
        if (distance > MAX_DISTANCE_METERS) {
            throw new ConflictException("VISIT_TOO_FAR", "visit.error.too_far",
                    Math.round(distance));
        }
        LocalDate today = LocalDate.now(VIETNAM_ZONE);
        if (visits.existsByUserIdAndPlaceIdAndVisitDate(userId, placeId, today)) {
            throw new ConflictException("VISIT_ALREADY_TODAY", "visit.error.already_today");
        }
        Visit visit = new Visit(userId, placeId, today,
                GeoUtils.toPoint(latitude, longitude), (int) Math.round(distance));
        try {
            visits.save(visit);
        } catch (DataIntegrityViolationException e) {
            // Hai request cùng lúc lọt qua exists-check: unique index trên bảng chặn.
            throw new ConflictException("VISIT_ALREADY_TODAY", "visit.error.already_today");
        }
        events.publishEvent(new VisitRecorded(placeId, visit.getId()));
        return toDto(visit, place.name());
    }

    /** Lịch sử đã đến của chính mình, mới nhất trước. */
    @Transactional(readOnly = true)
    public PageResponse<VisitDto> listOwnVisits(UUID userId, int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new BadRequestException("VALIDATION_FAILED", "common.error.validation");
        }
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        var pageResult = visits.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        List<UUID> placeIds = pageResult.getContent().stream()
                .map(Visit::getPlaceId).distinct().toList();
        Map<UUID, String> namesById = places.getSummaries(placeIds, "vi").stream()
                .collect(Collectors.toMap(summary -> summary.id(), summary -> summary.name()));
        return PageResponse.from(pageResult,
                visit -> toDto(visit, namesById.get(visit.getPlaceId())));
    }

    @Transactional(readOnly = true)
    @Override
    public VisitCounts countsForPlace(UUID placeId) {
        return new VisitCounts(
                visits.countByPlaceId(placeId),
                visits.countDistinctUserIdByPlaceId(placeId));
    }

    private VisitDto toDto(Visit visit, String placeName) {
        return new VisitDto(
                visit.getId(),
                visit.getPlaceId(),
                placeName,
                visit.getVisitDate(),
                visit.getDistanceMeters(),
                visit.getCreatedAt());
    }
}
