package com.foodmap.visit;

import com.foodmap.place.api.PlaceApi;
import com.foodmap.visit.api.VisitApi;
import com.foodmap.visit.api.VisitCounts;
import com.foodmap.visit.api.VisitRecorded;
import com.foodmap.visit.application.VisitPlaceAggregateListener;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Listener đồng bộ số liệu ghé thăm sang place. */
class VisitPlaceAggregateListenerTest {

    @Test
    @DisplayName("VisitRecorded kéo số liệu mới về place")
    void syncsCountsOnVisitRecorded() {
        UUID placeId = UUID.randomUUID();
        VisitApi visitApi = mock(VisitApi.class);
        PlaceApi placeApi = mock(PlaceApi.class);
        when(visitApi.countsForPlace(placeId)).thenReturn(new VisitCounts(7, 5));
        var listener = new VisitPlaceAggregateListener(visitApi, placeApi);

        listener.on(new VisitRecorded(placeId, UUID.randomUUID()));

        verify(placeApi).updateVisitStats(placeId, 7, 5);
    }
}
