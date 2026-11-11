package com.foodmap.review;

import com.foodmap.place.api.PlaceApi;
import com.foodmap.review.api.ReviewApi;
import com.foodmap.review.api.ReviewCreated;
import com.foodmap.review.api.ReviewModerated;
import com.foodmap.review.api.ReviewStats;
import com.foodmap.review.api.ReviewStatus;
import com.foodmap.review.application.ReviewPlaceAggregateListener;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Listener đồng bộ số liệu sang place: tính lại từ review đã duyệt rồi áp qua PlaceApi. */
class ReviewPlaceAggregateListenerTest {

    private static final UUID PLACE_ID = UUID.randomUUID();

    @Test
    @DisplayName("ReviewCreated và ReviewModerated đều kéo số liệu mới về place")
    void syncsStatsOnReviewEvents() {
        ReviewApi reviewApi = mock(ReviewApi.class);
        PlaceApi placeApi = mock(PlaceApi.class);
        when(reviewApi.statsForPlace(PLACE_ID))
                .thenReturn(new ReviewStats(new BigDecimal("4.5"), 2));
        var listener = new ReviewPlaceAggregateListener(reviewApi, placeApi);

        listener.on(new ReviewCreated(PLACE_ID, UUID.randomUUID()));
        listener.on(new ReviewModerated(PLACE_ID, UUID.randomUUID(), ReviewStatus.APPROVED));

        // Hai sự kiện → hai lần đồng bộ (mỗi lần tính lại toàn bộ, không cộng dồn).
        verify(placeApi, org.mockito.Mockito.times(2))
                .updateRatingStats(PLACE_ID, new BigDecimal("4.5"), 2);
        verify(reviewApi, org.mockito.Mockito.times(2)).statsForPlace(PLACE_ID);
    }
}
