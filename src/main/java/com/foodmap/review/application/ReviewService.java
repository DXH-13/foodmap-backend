package com.foodmap.review.application;

import com.foodmap.common.BadRequestException;
import com.foodmap.common.ConflictException;
import com.foodmap.common.ForbiddenException;
import com.foodmap.common.NotFoundException;
import com.foodmap.common.PageResponse;
import com.foodmap.identity.api.IdentityApi;
import com.foodmap.identity.api.UserProfile;
import com.foodmap.place.api.PlaceApi;
import com.foodmap.review.api.ModerateAction;
import com.foodmap.review.api.ReviewApi;
import com.foodmap.review.api.ReviewCreated;
import com.foodmap.review.api.ReviewDeleted;
import com.foodmap.review.api.ReviewModerated;
import com.foodmap.review.api.ReviewStats;
import com.foodmap.review.api.ReviewStatus;
import com.foodmap.review.api.ReviewUpdated;
import com.foodmap.review.domain.Review;
import com.foodmap.review.infrastructure.ReviewRepository;
import com.foodmap.review.web.dto.ModerateReviewRequest;
import com.foodmap.review.web.dto.ModerationReviewDto;
import com.foodmap.review.web.dto.ReviewAuthorDto;
import com.foodmap.review.web.dto.ReviewDto;
import com.foodmap.review.web.dto.SubmitReviewRequest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Use case đánh giá (Phase 2): gửi/cập nhật, xem, xoá đánh giá của mình và
 * kiểm duyệt cho admin.
 *
 * <p>Đánh giá mới luôn bắt đầu ở {@code PENDING}; chỉ đánh giá đã duyệt mới
 * hiện công khai và mới tính vào điểm trung bình. Mọi thay đổi trạng thái đều
 * phát sự kiện để đồng bộ số liệu sang {@code place} qua outbox.
 */
@Service
public class ReviewService implements ReviewApi {

    private final ReviewRepository reviews;
    private final PlaceApi places;
    private final IdentityApi identity;
    private final ApplicationEventPublisher events;

    public ReviewService(ReviewRepository reviews,
                         PlaceApi places,
                         IdentityApi identity,
                         ApplicationEventPublisher events) {
        this.reviews = reviews;
        this.places = places;
        this.identity = identity;
        this.events = events;
    }

    /** Kết quả gửi đánh giá: {@code created} phân biệt 201 (mới) với 200 (cập nhật). */
    public record SubmitOutcome(ReviewDto review, boolean created) {
    }

    /**
     * Gửi hoặc cập nhật đánh giá cho một địa điểm. Mỗi người chỉ có một đánh giá
     * cho mỗi địa điểm — gửi lại là cập nhật bản cũ, không tạo bản ghi mới.
     */
    @Transactional
    public SubmitOutcome submitReview(UUID placeId, UUID userId, boolean emailVerified,
                                      SubmitReviewRequest request) {
        if (!emailVerified) {
            throw new ForbiddenException("EMAIL_NOT_VERIFIED", "auth.error.email_not_verified");
        }
        if (!places.existsById(placeId)) {
            throw new NotFoundException("PLACE_NOT_FOUND", "place.error.not_found");
        }
        String locale = requestLocale();
        return reviews.findByPlaceIdAndUserIdAndDeletedAtIsNull(placeId, userId)
                .map(existing -> {
                    existing.resubmit(request.rating(), request.content());
                    reviews.save(existing);
                    events.publishEvent(new ReviewUpdated(placeId, existing.getId()));
                    return new SubmitOutcome(toDto(existing, true), false);
                })
                .orElseGet(() -> {
                    Review created = Review.create(placeId, userId, request.rating(),
                            request.content(), locale);
                    reviews.save(created);
                    events.publishEvent(new ReviewCreated(placeId, created.getId()));
                    return new SubmitOutcome(toDto(created, true), true);
                });
    }

    /**
     * Đánh giá công khai của một địa điểm: chỉ review đã duyệt, cộng thêm review
     * của chính người gọi (mọi trạng thái) khi người gọi đã đăng nhập.
     */
    @Transactional(readOnly = true)
    public PageResponse<ReviewDto> listPlaceReviews(UUID placeId, String sort,
                                                    int page, int size, UUID callerId) {
        requireValidPage(page, size);
        if (!places.existsById(placeId)) {
            throw new NotFoundException("PLACE_NOT_FOUND", "place.error.not_found");
        }
        Pageable pageable = PageRequest.of(page, size, placeReviewSort(sort));
        return PageResponse.from(
                reviews.findVisibleByPlaceId(placeId, callerId, pageable),
                review -> toDto(review, callerId != null && callerId.equals(review.getUserId())));
    }

    /** Đánh giá của chính mình, mọi trạng thái, mới nhất trước. */
    @Transactional(readOnly = true)
    public PageResponse<ReviewDto> listOwnReviews(UUID userId, int page, int size) {
        requireValidPage(page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return PageResponse.from(reviews.findByUserIdAndDeletedAtIsNull(userId, pageable),
                review -> toDto(review, true));
    }

    /** Xoá đánh giá của chính mình (xoá mềm). */
    @Transactional
    public void deleteOwnReview(UUID reviewId, UUID userId) {
        Review review = requireReview(reviewId);
        if (!review.getUserId().equals(userId)) {
            throw new ForbiddenException("NOT_OWN_REVIEW", "review.error.not_own_review");
        }
        review.softDelete();
        reviews.save(review);
        events.publishEvent(new ReviewDeleted(review.getPlaceId(), review.getId()));
    }

    /** Hàng chờ kiểm duyệt cho admin: lọc theo trạng thái / địa điểm. */
    @Transactional(readOnly = true)
    public PageResponse<ModerationReviewDto> listForModeration(ReviewStatus status, UUID placeId,
                                                               String sort, int page, int size) {
        requireValidPage(page, size);
        Sort sortOrder = "NEWEST_FIRST".equals(sort)
                ? Sort.by(Sort.Direction.DESC, "createdAt")
                : Sort.by(Sort.Direction.ASC, "createdAt");
        Pageable pageable = PageRequest.of(page, size, sortOrder);
        return PageResponse.from(
                reviews.findForModeration(status, placeId, pageable),
                review -> {
                    String placeName;
                    try {
                        placeName = places.getPlace(review.getPlaceId()).name();
                    } catch (NotFoundException e) {
                        placeName = "(đã xoá)";
                    }
                    long rejectedCount = reviews.countByUserIdAndStatusAndDeletedAtIsNull(
                            review.getUserId(), ReviewStatus.REJECTED);
                    return ModerationReviewDto.from(toDto(review, true), placeName, rejectedCount);
                });
    }

    /**
     * Duyệt / từ chối / ẩn / bỏ ẩn một đánh giá.
     *
     * <p>409 {@code REVIEW_ALREADY_MODERATED} khi đánh giá đã có quyết định kiểm
     * duyệt của <b>người khác</b> — cùng một moderator sửa lại quyết định của
     * mình thì được. Đây là cách đọc mô tả "Đánh giá đã được người khác xử lý"
     * trong hợp đồng; hợp đồng không nói rõ nên ghi lại ở đây.
     */
    @Transactional
    public ReviewDto moderateReview(UUID reviewId, UUID moderatorId, ModerateReviewRequest request) {
        Review review = requireReview(reviewId);
        if (request.action() == ModerateAction.REJECT
                && (request.moderationNote() == null || request.moderationNote().isBlank())) {
            throw new BadRequestException(
                    "MODERATION_NOTE_REQUIRED", "review.error.moderation_note_required");
        }
        if (review.getStatus() != ReviewStatus.PENDING
                && review.getModeratedBy() != null
                && !review.getModeratedBy().equals(moderatorId)) {
            throw new ConflictException("REVIEW_ALREADY_MODERATED", "review.error.already_moderated");
        }
        ReviewStatus target = switch (request.action()) {
            case APPROVE -> ReviewStatus.APPROVED;
            case REJECT -> ReviewStatus.REJECTED;
            case HIDE -> ReviewStatus.HIDDEN;
            case UNHIDE -> {
                if (review.getStatus() != ReviewStatus.HIDDEN) {
                    throw new ConflictException("REVIEW_NOT_HIDDEN", "review.error.not_hidden");
                }
                yield ReviewStatus.APPROVED;
            }
        };
        review.moderate(target, request.moderationNote(), moderatorId);
        reviews.save(review);
        events.publishEvent(new ReviewModerated(review.getPlaceId(), review.getId(), target));
        return toDto(review, true);
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<ReviewDetails> findReview(UUID reviewId) {
        return reviews.findByIdAndDeletedAtIsNull(reviewId)
                .map(review -> new ReviewDetails(
                        review.getId(), review.getPlaceId(), review.getUserId(), review.getStatus()));
    }

    @Transactional(readOnly = true)
    @Override
    public ReviewStats statsForPlace(UUID placeId) {
        Object[] row = reviews.aggregateApprovedByPlaceId(placeId);
        Double average = (Double) row[0];
        long count = (Long) row[1];
        BigDecimal averageRating = (count == 0 || average == null) ? null
                : BigDecimal.valueOf(average).setScale(1, RoundingMode.HALF_UP);
        return new ReviewStats(averageRating, (int) count);
    }

    private Review requireReview(UUID reviewId) {
        return reviews.findByIdAndDeletedAtIsNull(reviewId)
                .orElseThrow(() -> new NotFoundException("REVIEW_NOT_FOUND", "review.error.not_found"));
    }

    /**
     * Controller không có {@code @Validated} nên {@code @Min}/{@code @Max} trên
     * {@code @RequestParam} chỉ là tài liệu — kiểm tra lại ở đây để page/size
     * sai không rơi thành lỗi 500 của {@code PageRequest}.
     */
    private void requireValidPage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new BadRequestException("VALIDATION_FAILED", "common.error.validation");
        }
    }

    private Sort placeReviewSort(String sort) {
        return switch (sort == null ? "NEWEST" : sort) {
            case "HIGHEST_RATING" -> Sort.by(Sort.Order.desc("rating"), Sort.Order.desc("createdAt"));
            case "LOWEST_RATING" -> Sort.by(Sort.Order.asc("rating"), Sort.Order.desc("createdAt"));
            case "NEWEST" -> Sort.by(Sort.Direction.DESC, "createdAt");
            default -> throw new BadRequestException("VALIDATION_FAILED", "common.error.validation");
        };
    }

    private String requestLocale() {
        String language = LocaleContextHolder.getLocale().getLanguage();
        return "en".equals(language) ? "en" : "vi";
    }

    private ReviewDto toDto(Review review, boolean includeModerationNote) {
        return new ReviewDto(
                review.getId(),
                review.getPlaceId(),
                authorOf(review.getUserId()),
                review.getRating(),
                review.getContent(),
                review.getLocale(),
                review.getStatus().name(),
                includeModerationNote ? review.getModerationNote() : null,
                List.of(),
                review.getCreatedAt(),
                review.getUpdatedAt());
    }

    /**
     * Tác giả hiển thị của một đánh giá. Người dùng đã xoá mềm tài khoản vẫn có
     * đánh giá hiển thị được — dùng tên giữ chỗ thay vì làm hỏng cả danh sách.
     */
    private ReviewAuthorDto authorOf(UUID userId) {
        try {
            UserProfile profile = identity.getUser(userId);
            return new ReviewAuthorDto(profile.id(), profile.displayName(), profile.avatarUrl());
        } catch (NotFoundException e) {
            return new ReviewAuthorDto(userId, "Người dùng đã rời đi", null);
        }
    }
}
