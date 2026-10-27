package com.foodmap.review.web;

import com.foodmap.auth.AuthPrincipal;
import com.foodmap.common.PageResponse;
import com.foodmap.review.ReviewService;
import com.foodmap.review.dto.ReviewDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@Tag(name = "review", description = "Đánh giá địa điểm")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/api/v1/reviews/feed")
    @Operation(
            operationId = "listReviewFeed",
            summary = "Bảng tin bài đánh giá",
            description = "Đánh giá APPROVED của mọi địa điểm đang hiển thị, mới nhất trước. Không cần đăng nhập.")
    public PageResponse<ReviewDto> listReviewFeed(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return reviewService.listFeed(PageRequest.of(page, size));
    }

    @GetMapping("/api/v1/places/{placeId}/reviews")
    @Operation(
            operationId = "listPlaceReviews",
            summary = "Lấy đánh giá của một địa điểm",
            description = "Chỉ trả review đã duyệt, trừ review của chính người gọi (luôn thấy kèm nhãn trạng thái).")
    public PageResponse<ReviewDto> listPlaceReviews(
            @PathVariable UUID placeId,
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestParam(defaultValue = "NEWEST") ReviewService.PlaceReviewSort sort,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return reviewService.listForPlace(
                placeId, principal == null ? null : principal.userId(), sort, PageRequest.of(page, size));
    }

    @GetMapping("/api/v1/me/reviews")
    @Operation(operationId = "listOwnReviews", summary = "Lấy đánh giá của chính mình")
    public PageResponse<ReviewDto> listOwnReviews(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return reviewService.listOwn(principal.userId(), PageRequest.of(page, size));
    }
}
