package com.foodmap.review;

import com.foodmap.common.BadRequestException;
import com.foodmap.common.ConflictException;
import com.foodmap.common.ForbiddenException;
import com.foodmap.common.NotFoundException;
import com.foodmap.identity.api.IdentityApi;
import com.foodmap.identity.api.UserProfile;
import com.foodmap.identity.api.UserRole;
import com.foodmap.place.api.PlaceApi;
import com.foodmap.review.api.ReviewCreated;
import com.foodmap.review.api.ReviewModerated;
import com.foodmap.review.api.ReviewStatus;
import com.foodmap.review.api.ReviewUpdated;
import com.foodmap.review.application.ReviewService;
import com.foodmap.review.domain.Review;
import com.foodmap.review.infrastructure.ReviewRepository;
import com.foodmap.review.web.dto.ModerateReviewRequest;
import com.foodmap.review.web.dto.SubmitReviewRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test use case đánh giá ở tầng service, không cần CSDL hay Docker.
 * Repository và {@code ApplicationEventPublisher} là mock; entity là thật.
 */
class ReviewServiceTest {

    private static final UUID PLACE_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID MODERATOR_ID = UUID.randomUUID();

    private ReviewRepository reviews;
    private PlaceApi places;
    private IdentityApi identity;
    private ApplicationEventPublisher events;
    private ReviewService reviewService;

    @BeforeEach
    void setUp() {
        reviews = mock(ReviewRepository.class);
        places = mock(PlaceApi.class);
        identity = mock(IdentityApi.class);
        events = mock(ApplicationEventPublisher.class);
        reviewService = new ReviewService(reviews, places, identity, events);
        when(places.existsById(PLACE_ID)).thenReturn(true);
        // Giả lập JPA persist: save() gán id cho entity.
        when(reviews.save(any(Review.class))).thenAnswer(invocation -> {
            Review review = invocation.getArgument(0);
            if (review.getId() == null) {
                review.setId(UUID.randomUUID());
            }
            return review;
        });
        when(identity.getUser(USER_ID)).thenReturn(new UserProfile(
                USER_ID, "a@foodmap.vn", "An", null, UserRole.USER, "vi", true, Instant.now()));
    }

    private Review approvedReview() {
        Review review = Review.create(PLACE_ID, USER_ID, 5, "Ngon", "vi");
        review.setId(UUID.randomUUID());
        review.moderate(ReviewStatus.APPROVED, null, MODERATOR_ID);
        return review;
    }

    @Test
    @DisplayName("Gửi mới: tạo review PENDING, trả 201, phát ReviewCreated")
    void submitCreatesPendingReview() {
        when(reviews.findByPlaceIdAndUserIdAndDeletedAtIsNull(PLACE_ID, USER_ID))
                .thenReturn(Optional.empty());

        ReviewService.SubmitOutcome outcome = reviewService.submitReview(
                PLACE_ID, USER_ID, true, new SubmitReviewRequest(4, "Ổn", null));

        assertThat(outcome.created()).isTrue();
        assertThat(outcome.review().status()).isEqualTo("PENDING");
        assertThat(outcome.review().rating()).isEqualTo(4);
        verify(reviews).save(any(Review.class));
        verify(events).publishEvent(any(ReviewCreated.class));
    }

    @Test
    @DisplayName("Gửi lại review đã duyệt: cập nhật nội dung, quay về PENDING, phát ReviewUpdated")
    void resubmitApprovedReviewGoesBackToPending() {
        Review existing = approvedReview();
        when(reviews.findByPlaceIdAndUserIdAndDeletedAtIsNull(PLACE_ID, USER_ID))
                .thenReturn(Optional.of(existing));

        ReviewService.SubmitOutcome outcome = reviewService.submitReview(
                PLACE_ID, USER_ID, true, new SubmitReviewRequest(3, "Đổi ý", null));

        assertThat(outcome.created()).isFalse();
        assertThat(existing.getStatus()).isEqualTo(ReviewStatus.PENDING);
        assertThat(existing.getRating()).isEqualTo(3);
        assertThat(existing.getModeratedBy()).isNull();
        verify(events).publishEvent(any(ReviewUpdated.class));
        verify(events, never()).publishEvent(any(ReviewCreated.class));
    }

    @Test
    @DisplayName("Email chưa xác minh: 403 EMAIL_NOT_VERIFIED, không lưu gì")
    void submitRequiresVerifiedEmail() {
        assertThatThrownBy(() -> reviewService.submitReview(
                PLACE_ID, USER_ID, false, new SubmitReviewRequest(5, "Ngon", null)))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("EMAIL_NOT_VERIFIED");
        verify(reviews, never()).save(any(Review.class));
    }

    @Test
    @DisplayName("Địa điểm không tồn tại: 404 PLACE_NOT_FOUND")
    void submitRequiresExistingPlace() {
        when(places.existsById(PLACE_ID)).thenReturn(false);

        assertThatThrownBy(() -> reviewService.submitReview(
                PLACE_ID, USER_ID, true, new SubmitReviewRequest(5, "Ngon", null)))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("PLACE_NOT_FOUND");
    }

    @Test
    @DisplayName("Xoá review của người khác: 403, không xoá")
    void deleteRequiresOwnership() {
        Review review = approvedReview();
        when(reviews.findByIdAndDeletedAtIsNull(review.getId())).thenReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.deleteOwnReview(review.getId(), UUID.randomUUID()))
                .isInstanceOf(ForbiddenException.class);
        assertThat(review.getDeletedAt()).isNull();
        verify(events, never()).publishEvent(any());
    }

    @Test
    @DisplayName("Từ chối không kèm lý do: 400 MODERATION_NOTE_REQUIRED")
    void rejectRequiresNote() {
        Review review = Review.create(PLACE_ID, USER_ID, 2, "Tệ", "vi");
        review.setId(UUID.randomUUID());
        when(reviews.findByIdAndDeletedAtIsNull(review.getId())).thenReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.moderateReview(
                review.getId(), MODERATOR_ID,
                new ModerateReviewRequest(com.foodmap.review.api.ModerateAction.REJECT, null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("MODERATION_NOTE_REQUIRED");
    }

    @Test
    @DisplayName("Moderator khác xử lý lại review đã duyệt: 409 REVIEW_ALREADY_MODERATED")
    void anotherModeratorCannotReModerate() {
        Review review = approvedReview();
        when(reviews.findByIdAndDeletedAtIsNull(review.getId())).thenReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.moderateReview(
                review.getId(), UUID.randomUUID(),
                new ModerateReviewRequest(com.foodmap.review.api.ModerateAction.REJECT, "Spam")))
                .isInstanceOf(ConflictException.class)
                .hasMessage("REVIEW_ALREADY_MODERATED");
    }

    @Test
    @DisplayName("Bỏ ẩn review không bị ẩn: 409 REVIEW_NOT_HIDDEN")
    void unhideRequiresHidden() {
        Review review = approvedReview();
        when(reviews.findByIdAndDeletedAtIsNull(review.getId())).thenReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.moderateReview(
                review.getId(), MODERATOR_ID,
                new ModerateReviewRequest(com.foodmap.review.api.ModerateAction.UNHIDE, null)))
                .isInstanceOf(ConflictException.class)
                .hasMessage("REVIEW_NOT_HIDDEN");
    }

    @Test
    @DisplayName("Duyệt review đang chờ: APPROVED, phát ReviewModerated")
    void approvePendingReview() {
        Review review = Review.create(PLACE_ID, USER_ID, 5, "Ngon", "vi");
        review.setId(UUID.randomUUID());
        when(reviews.findByIdAndDeletedAtIsNull(review.getId())).thenReturn(Optional.of(review));

        var dto = reviewService.moderateReview(
                review.getId(), MODERATOR_ID,
                new ModerateReviewRequest(com.foodmap.review.api.ModerateAction.APPROVE, null));

        assertThat(dto.status()).isEqualTo("APPROVED");
        assertThat(review.getModeratedBy()).isEqualTo(MODERATOR_ID);
        verify(events).publishEvent(any(ReviewModerated.class));
    }

    @Test
    @DisplayName("statsForPlace chỉ tính review đã duyệt, làm tròn 1 chữ số thập phân")
    void statsCountsOnlyApproved() {
        when(reviews.aggregateApprovedByPlaceId(PLACE_ID))
                .thenReturn(new Object[]{4.55d, 3L});

        var stats = reviewService.statsForPlace(PLACE_ID);

        assertThat(stats.averageRating()).isEqualTo(new BigDecimal("4.6"));
        assertThat(stats.reviewCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("statsForPlace khi chưa có review đã duyệt: average null, count 0")
    void statsEmptyWhenNoApproved() {
        when(reviews.aggregateApprovedByPlaceId(PLACE_ID))
                .thenReturn(new Object[]{null, 0L});

        var stats = reviewService.statsForPlace(PLACE_ID);

        assertThat(stats.averageRating()).isNull();
        assertThat(stats.reviewCount()).isZero();
    }

    @Test
    @DisplayName("Danh sách công khai: moderationNote chỉ hiện với review của chính người gọi")
    void publicListHidesModerationNoteFromOthers() {
        UUID otherUser = UUID.randomUUID();
        Review mine = Review.create(PLACE_ID, USER_ID, 5, "Của tôi", "vi");
        mine.setId(UUID.randomUUID());
        mine.moderate(ReviewStatus.PENDING, "Chờ duyệt", MODERATOR_ID);
        Review theirs = Review.create(PLACE_ID, otherUser, 4, "Của người khác", "vi");
        theirs.setId(UUID.randomUUID());
        theirs.moderate(ReviewStatus.APPROVED, "Đã duyệt", MODERATOR_ID);
        when(reviews.findVisibleByPlaceId(eq(PLACE_ID), eq(USER_ID), any()))
                .thenReturn(new PageImpl<>(List.of(mine, theirs)));
        when(identity.getUser(otherUser)).thenReturn(new UserProfile(
                otherUser, "b@foodmap.vn", "Bình", null, UserRole.USER, "vi", true, Instant.now()));

        var page = reviewService.listPlaceReviews(PLACE_ID, "NEWEST", 0, 20, USER_ID);

        assertThat(page.content()).hasSize(2);
        assertThat(page.content().get(0).moderationNote()).isEqualTo("Chờ duyệt");
        assertThat(page.content().get(1).moderationNote()).isNull();
    }
}
