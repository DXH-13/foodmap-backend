package com.foodmap.review.web;

import com.foodmap.common.PageResponse;
import com.foodmap.identity.api.AuthPrincipal;
import com.foodmap.review.application.ReviewService;
import com.foodmap.review.web.dto.ReviewDto;
import com.foodmap.review.web.dto.SubmitReviewRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Đánh giá — phía người dùng. Khớp các operation {@code submitReview},
 * {@code listPlaceReviews}, {@code deleteOwnReview}, {@code listOwnReviews}
 * trong hợp đồng.
 *
 * <p>Lưu ý: hợp đồng <b>không có</b> PATCH cho review — cập nhật đánh giá làm
 * bằng cách gửi lại POST (upsert), trả 200 khi cập nhật và 201 khi tạo mới.
 */
@RestController
@Tag(name = "review", description = "Đánh giá địa điểm")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping("/app/v1/places/{placeId}/reviews")
    @Operation(operationId = "submitReview", summary = "Gửi hoặc cập nhật đánh giá cho một địa điểm")
    public ResponseEntity<ReviewDto> submitReview(@PathVariable UUID placeId,
                                                  @AuthenticationPrincipal AuthPrincipal principal,
                                                  @Valid @RequestBody SubmitReviewRequest request) {
        ReviewService.SubmitOutcome outcome = reviewService.submitReview(
                placeId, principal.userId(), principal.emailVerified(), request);
        return outcome.created()
                ? ResponseEntity.status(201).body(outcome.review())
                : ResponseEntity.ok(outcome.review());
    }

    @GetMapping("/app/v1/places/{placeId}/reviews")
    @Operation(operationId = "listPlaceReviews", summary = "Lấy đánh giá của một địa điểm")
    public PageResponse<ReviewDto> listPlaceReviews(
            @PathVariable UUID placeId,
            @RequestParam(defaultValue = "NEWEST") String sort,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @AuthenticationPrincipal AuthPrincipal principal) {
        UUID callerId = principal == null ? null : principal.userId();
        return reviewService.listPlaceReviews(placeId, sort, page, size, callerId);
    }

    @DeleteMapping("/app/v1/reviews/{reviewId}")
    @Operation(operationId = "deleteOwnReview", summary = "Xoá đánh giá của chính mình")
    public ResponseEntity<Void> deleteOwnReview(@PathVariable UUID reviewId,
                                                @AuthenticationPrincipal AuthPrincipal principal) {
        reviewService.deleteOwnReview(reviewId, principal.userId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/app/v1/me/reviews")
    @Operation(operationId = "listOwnReviews", summary = "Lấy đánh giá của chính mình")
    public PageResponse<ReviewDto> listOwnReviews(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return reviewService.listOwnReviews(principal.userId(), page, size);
    }
}
