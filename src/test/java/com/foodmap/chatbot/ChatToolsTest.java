package com.foodmap.chatbot;

import com.foodmap.chatbot.application.ChatTools;
import com.foodmap.chatbot.application.ChatTools.ToolContext;
import com.foodmap.chatbot.application.FunctionCall;
import com.foodmap.place.api.PlaceApi;
import com.foodmap.place.api.PlaceSummary;
import com.foodmap.review.api.ReviewApi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Kiểm thử {@link ChatTools} với PlaceApi/ReviewApi bị mock — không cần DB hay LLM. */
@ExtendWith(MockitoExtension.class)
class ChatToolsTest {

    @Mock
    PlaceApi placeApi;

    @Mock
    ReviewApi reviewApi;

    UUID placeId = UUID.randomUUID();

    PlaceSummary summary() {
        return new PlaceSummary(placeId, "pho-24", "Phở 24", "QUAN_AN",
                10.7769, 106.7009, "12 Lê Lợi", "PUBLISHED",
                BigDecimal.valueOf(4.5), 120, 350L);
    }

    ToolContext context() {
        return new ToolContext(10.7769, 106.7009, "vi");
    }

    @Test
    void definitions_exposesThreeTools() {
        ChatTools tools = new ChatTools(placeApi, reviewApi);
        assertThat(tools.definitions()).extracting(d -> d.name())
                .containsExactlyInAnyOrder("search_places", "get_place_details", "list_place_reviews");
    }

    @Test
    void searchPlaces_withCoordinates_usesNearbySearch_andCollectsPlaceIds() {
        when(placeApi.searchNearby(anyDouble(), anyDouble(), anyInt(), anyInt()))
                .thenReturn(List.of(summary()));
        ChatTools tools = new ChatTools(placeApi, reviewApi);
        var outcome = tools.execute(new FunctionCall("search_places",
                Map.of("query", "phở", "latitude", 10.7769, "longitude", 106.7009,
                        "radiusMeters", 2000, "limit", 5)), context());
        verify(placeApi).searchNearby(10.7769, 106.7009, 2000, 5);
        assertThat(outcome.placeIds()).containsExactly(placeId);
        @SuppressWarnings("unchecked")
        List<?> places = (List<?>) outcome.response().get("places");
        assertThat(places).hasSize(1);
        // Khoảng cách tính bằng Haversine từ tọa độ user tới tọa độ quán (~0 m),
        // Math.round nên ra Long.
        @SuppressWarnings("unchecked")
        Map<String, Object> place = (Map<String, Object>) places.get(0);
        assertThat(((Number) place.get("distanceMeters")).doubleValue()).isLessThan(1.0);
    }

    @Test
    void searchPlaces_withQueryOnly_usesKeywordSearch() {
        when(placeApi.searchByKeyword(anyString(), anyInt())).thenReturn(List.of(summary()));
        ChatTools tools = new ChatTools(placeApi, reviewApi);
        var outcome = tools.execute(new FunctionCall("search_places",
                Map.of("query", "cơm tấm", "limit", 5)), context());
        verify(placeApi).searchByKeyword("cơm tấm", 5);
        assertThat(outcome.placeIds()).containsExactly(placeId);
    }

    @Test
    void searchPlaces_withNeither_returnsEmptyAndNote() {
        ChatTools tools = new ChatTools(placeApi, reviewApi);
        var outcome = tools.execute(new FunctionCall("search_places", Map.of("limit", 5)), context());
        assertThat(outcome.placeIds()).isEmpty();
        assertThat(outcome.response()).containsKey("note");
    }

    @Test
    void getPlaceDetails_found_returnsPlace() {
        when(placeApi.getSummaries(List.of(placeId), "vi")).thenReturn(List.of(summary()));
        ChatTools tools = new ChatTools(placeApi, reviewApi);
        var outcome = tools.execute(new FunctionCall("get_place_details",
                Map.of("placeId", placeId.toString())), context());
        assertThat(outcome.placeIds()).containsExactly(placeId);
        assertThat(outcome.response()).containsKey("place");
    }

    @Test
    void getPlaceDetails_invalidUuid_returnsError() {
        ChatTools tools = new ChatTools(placeApi, reviewApi);
        var outcome = tools.execute(new FunctionCall("get_place_details",
                Map.of("placeId", "khong-phai-uuid")), context());
        assertThat(outcome.response()).containsKey("error");
    }

    @Test
    void getPlaceDetails_unknownPlace_returnsError() {
        when(placeApi.getSummaries(List.of(placeId), "vi")).thenReturn(List.of());
        ChatTools tools = new ChatTools(placeApi, reviewApi);
        var outcome = tools.execute(new FunctionCall("get_place_details",
                Map.of("placeId", placeId.toString())), context());
        assertThat(outcome.response()).containsKey("error");
        assertThat(outcome.placeIds()).isEmpty();
    }

    @Test
    void listPlaceReviews_returnsApprovedReviews() {
        when(reviewApi.listApprovedReviews(eq(placeId), anyInt())).thenReturn(List.of(
                new ReviewApi.ReviewSummary(UUID.randomUUID(), 5, "Ngon!", Instant.now())));
        ChatTools tools = new ChatTools(placeApi, reviewApi);
        var outcome = tools.execute(new FunctionCall("list_place_reviews",
                Map.of("placeId", placeId.toString(), "limit", 3)), context());
        @SuppressWarnings("unchecked")
        List<?> reviews = (List<?>) outcome.response().get("reviews");
        assertThat(reviews).hasSize(1);
        assertThat(outcome.placeIds()).containsExactly(placeId);
    }

    @Test
    void unknownTool_returnsError() {
        ChatTools tools = new ChatTools(placeApi, reviewApi);
        var outcome = tools.execute(new FunctionCall("khong_co_tool", Map.of()), context());
        assertThat(outcome.response()).containsKey("error");
    }
}
