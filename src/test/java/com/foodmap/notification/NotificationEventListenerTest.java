package com.foodmap.notification;

import com.foodmap.identity.api.UserRegistered;
import com.foodmap.notification.api.NotificationApi;
import com.foodmap.notification.api.NotificationType;
import com.foodmap.notification.application.NotificationEventListener;
import com.foodmap.place.api.PlaceApi;
import com.foodmap.place.api.PlaceDetails;
import com.foodmap.review.api.ReviewApi;
import com.foodmap.review.api.ReviewCreated;
import com.foodmap.review.api.ReviewModerated;
import com.foodmap.review.api.ReviewStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test {@link NotificationEventListener} — mock {@link NotificationApi} và
 * các {@code api/} của module khác, không cần CSDL.
 */
class NotificationEventListenerTest {

    private final NotificationApi notificationApi = mock(NotificationApi.class);
    private final ReviewApi reviews = mock(ReviewApi.class);
    private final PlaceApi places = mock(PlaceApi.class);

    private final NotificationEventListener listener =
            new NotificationEventListener(notificationApi, reviews, places);

    private static ReviewApi.ReviewDetails review(UUID reviewId, UUID placeId,
                                                  UUID authorId, ReviewStatus status) {
        return new ReviewApi.ReviewDetails(reviewId, placeId, authorId, status);
    }

    @Test
    @DisplayName("Đăng ký tài khoản mới → chào mừng trong app")
    void userRegisteredSendsWelcome() {
        UUID userId = UUID.randomUUID();

        listener.on(new UserRegistered(userId, "an@example.com"));

        verify(notificationApi).notify(eq(userId), eq(NotificationType.SYSTEM_ANNOUNCEMENT),
                eq("notification.welcome.title"), eq(Map.of()));
    }

    @Test
    @DisplayName("Đánh giá mới → báo chủ quán (không phải tác giả)")
    void reviewCreatedNotifiesPlaceCreator() {
        UUID placeId = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        when(reviews.findReview(reviewId))
                .thenReturn(Optional.of(review(reviewId, placeId, authorId, ReviewStatus.PENDING)));
        when(places.findCreatorById(placeId)).thenReturn(Optional.of(creatorId));
        when(places.getPlace(placeId))
                .thenReturn(new PlaceDetails(placeId, "Quán Ngon", 10.0, 106.0, "PUBLISHED"));

        listener.on(new ReviewCreated(placeId, reviewId));

        verify(notificationApi).notify(eq(creatorId), eq(NotificationType.SYSTEM_ANNOUNCEMENT),
                eq("notification.new_review.title"), any());
    }

    @Test
    @DisplayName("Đánh giá mới: tác giả là chủ quán thì không tự báo cho mình")
    void reviewCreatedSkipsSelfReview() {
        UUID placeId = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        when(reviews.findReview(reviewId))
                .thenReturn(Optional.of(review(reviewId, placeId, authorId, ReviewStatus.PENDING)));
        when(places.findCreatorById(placeId)).thenReturn(Optional.of(authorId));

        listener.on(new ReviewCreated(placeId, reviewId));

        verify(notificationApi, never()).notify(any(), any(), anyString(), any());
    }

    @Test
    @DisplayName("Đánh giá mới: quán không có người tạo thì không báo ai")
    void reviewCreatedSkipsPlaceWithoutCreator() {
        UUID placeId = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();
        when(reviews.findReview(reviewId)).thenReturn(Optional.of(
                review(reviewId, placeId, UUID.randomUUID(), ReviewStatus.PENDING)));
        when(places.findCreatorById(placeId)).thenReturn(Optional.empty());

        listener.on(new ReviewCreated(placeId, reviewId));

        verify(notificationApi, never()).notify(any(), any(), anyString(), any());
    }

    @Test
    @DisplayName("Kiểm duyệt duyệt → báo tác giả REVIEW_APPROVED")
    void reviewModeratedApprovedNotifiesAuthor() {
        UUID placeId = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        when(reviews.findReview(reviewId)).thenReturn(Optional.of(
                review(reviewId, placeId, authorId, ReviewStatus.APPROVED)));

        listener.on(new ReviewModerated(placeId, reviewId, ReviewStatus.APPROVED));

        verify(notificationApi).notify(eq(authorId), eq(NotificationType.REVIEW_APPROVED),
                eq("notification.review_approved.title"), any());
    }

    @Test
    @DisplayName("Kiểm duyệt từ chối → báo tác giả REVIEW_REJECTED")
    void reviewModeratedRejectedNotifiesAuthor() {
        UUID placeId = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        when(reviews.findReview(reviewId)).thenReturn(Optional.of(
                review(reviewId, placeId, authorId, ReviewStatus.REJECTED)));

        listener.on(new ReviewModerated(placeId, reviewId, ReviewStatus.REJECTED));

        verify(notificationApi).notify(eq(authorId), eq(NotificationType.REVIEW_REJECTED),
                eq("notification.review_rejected.title"), any());
    }

    @Test
    @DisplayName("Kiểm duyệt ẩn → không tạo thông báo (không có loại khớp)")
    void reviewModeratedHiddenSkipped() {
        UUID placeId = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();
        when(reviews.findReview(reviewId)).thenReturn(Optional.of(
                review(reviewId, placeId, UUID.randomUUID(), ReviewStatus.HIDDEN)));

        listener.on(new ReviewModerated(placeId, reviewId, ReviewStatus.HIDDEN));

        verify(notificationApi, never()).notify(any(), any(), anyString(), any());
    }

    @Test
    @DisplayName("Kiểm duyệt: đánh giá đã bị xoá thì không báo ai")
    void reviewModeratedSkipsDeletedReview() {
        UUID placeId = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();
        when(reviews.findReview(reviewId)).thenReturn(Optional.empty());

        listener.on(new ReviewModerated(placeId, reviewId, ReviewStatus.APPROVED));

        verify(notificationApi, never()).notify(any(), any(), anyString(), any());
    }
}
