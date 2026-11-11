package com.foodmap.visit;

import com.foodmap.common.ConflictException;
import com.foodmap.place.api.PlaceApi;
import com.foodmap.place.api.PlaceDetails;
import com.foodmap.visit.api.VisitRecorded;
import com.foodmap.visit.application.VisitService;
import com.foodmap.visit.domain.Visit;
import com.foodmap.visit.infrastructure.VisitRepository;
import com.foodmap.visit.web.dto.CoordinatesDto;
import com.foodmap.visit.web.dto.RecordVisitRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Test use case ghé thăm ở tầng service, không cần CSDL hay Docker. */
class VisitServiceTest {

    private static final UUID PLACE_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    private VisitRepository visits;
    private PlaceApi places;
    private ApplicationEventPublisher events;
    private VisitService visitService;

    @BeforeEach
    void setUp() {
        visits = mock(VisitRepository.class);
        places = mock(PlaceApi.class);
        events = mock(ApplicationEventPublisher.class);
        visitService = new VisitService(visits, places, events);
        when(places.getPlace(PLACE_ID))
                .thenReturn(new PlaceDetails(PLACE_ID, "Quán A", 10.0, 106.0, "PUBLISHED"));
        // Giả lập JPA persist: save() gán id cho entity.
        when(visits.save(any(Visit.class))).thenAnswer(invocation -> {
            Visit visit = invocation.getArgument(0);
            if (visit.getId() == null) {
                visit.setId(UUID.randomUUID());
            }
            return visit;
        });
    }

    private RecordVisitRequest at(double lat, double lng) {
        return new RecordVisitRequest(new CoordinatesDto(lat, lng));
    }

    @Test
    @DisplayName("Trong 200m, chưa đến hôm nay: ghi nhận 201, phát VisitRecorded, ngày theo giờ VN")
    void recordVisitSucceeds() {
        when(places.distanceMetersTo(PLACE_ID, 10.0, 106.0)).thenReturn(150.0);
        when(visits.existsByUserIdAndPlaceIdAndVisitDate(
                USER_ID, PLACE_ID, LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"))))
                .thenReturn(false);

        var dto = visitService.recordVisit(PLACE_ID, USER_ID, at(10.0, 106.0));

        assertThat(dto.placeId()).isEqualTo(PLACE_ID);
        assertThat(dto.placeName()).isEqualTo("Quán A");
        assertThat(dto.distanceMeters()).isEqualTo(150);
        assertThat(dto.visitDate()).isEqualTo(LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")));
        verify(visits).save(any(Visit.class));
        verify(events).publishEvent(any(VisitRecorded.class));
    }

    @Test
    @DisplayName("Quá 200m: 409 VISIT_TOO_FAR, không lưu")
    void recordVisitTooFar() {
        when(places.distanceMetersTo(PLACE_ID, 10.0, 106.0)).thenReturn(250.0);

        assertThatThrownBy(() -> visitService.recordVisit(PLACE_ID, USER_ID, at(10.0, 106.0)))
                .isInstanceOf(ConflictException.class)
                .hasMessage("VISIT_TOO_FAR");
        verify(visits, org.mockito.Mockito.never()).save(any(Visit.class));
    }

    @Test
    @DisplayName("Đã đến hôm nay: 409 VISIT_ALREADY_TODAY, không lưu")
    void recordVisitAlreadyToday() {
        when(places.distanceMetersTo(PLACE_ID, 10.0, 106.0)).thenReturn(50.0);
        when(visits.existsByUserIdAndPlaceIdAndVisitDate(
                USER_ID, PLACE_ID, LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"))))
                .thenReturn(true);

        assertThatThrownBy(() -> visitService.recordVisit(PLACE_ID, USER_ID, at(10.0, 106.0)))
                .isInstanceOf(ConflictException.class)
                .hasMessage("VISIT_ALREADY_TODAY");
    }

    @Test
    @DisplayName("countsForPlace: tổng lượt + số người khác nhau")
    void countsForPlace() {
        when(visits.countByPlaceId(PLACE_ID)).thenReturn(7L);
        when(visits.countDistinctUserIdByPlaceId(PLACE_ID)).thenReturn(5L);

        var counts = visitService.countsForPlace(PLACE_ID);

        assertThat(counts.visitCount()).isEqualTo(7);
        assertThat(counts.distinctVisitorCount()).isEqualTo(5);
    }
}
