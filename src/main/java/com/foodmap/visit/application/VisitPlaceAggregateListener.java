package com.foodmap.visit.application;

import com.foodmap.place.api.PlaceApi;
import com.foodmap.visit.api.VisitApi;
import com.foodmap.visit.api.VisitCounts;
import com.foodmap.visit.api.VisitRecorded;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Đồng bộ số lượt đến của địa điểm sau mỗi lượt đến được ghi nhận.
 *
 * <p>Cùng lý do như {@code ReviewPlaceAggregateListener}: listener nằm ở module
 * phát sự kiện để tránh phụ thuộc vòng tròn {@code visit → place} (gọi
 * {@code PlaceApi}) và {@code place → visit} (import kiểu sự kiện) — xem ghi
 * chú chi tiết ở đó.
 */
@Component
public class VisitPlaceAggregateListener {

    private final VisitApi visitApi;
    private final PlaceApi placeApi;

    public VisitPlaceAggregateListener(VisitApi visitApi, PlaceApi placeApi) {
        this.visitApi = visitApi;
        this.placeApi = placeApi;
    }

    @ApplicationModuleListener
    public void on(VisitRecorded event) {
        VisitCounts counts = visitApi.countsForPlace(event.placeId());
        placeApi.updateVisitStats(event.placeId(), counts.visitCount(), counts.distinctVisitorCount());
    }
}
