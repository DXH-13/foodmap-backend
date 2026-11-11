package com.foodmap.review.web;

import com.foodmap.common.PageResponse;
import com.foodmap.identity.api.AuthPrincipal;
import com.foodmap.review.api.ReviewStatus;
import com.foodmap.review.application.ReviewService;
import com.foodmap.review.web.dto.ModerateReviewRequest;
import com.foodmap.review.web.dto.ModerationReviewDto;
import com.foodmap.review.web.dto.ReviewDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Đánh giá — phía kiểm duyệt. Khớp các operation
 * {@code listReviewsForModeration} và {@code moderateReview} trong hợp đồng.
 *
 * <p>Quyền MODERATOR/ADMIN đã chặn ở {@code SecurityConfig} cho toàn bộ
 * {@code /admin/v1/**}; {@code @PreAuthorize} ở đây ghi rõ lại để đọc code
 * là biết, theo tiền lệ của {@code AdminUserController}.
 */
@RestController
@RequestMapping("/admin/v1/reviews")
@Tag(name = "admin", description = "Quản trị: kiểm duyệt, quản lý user, thống kê")
@PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
public class AdminReviewController {

    private final ReviewService reviewService;

    public AdminReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping
    @Operation(operationId = "listReviewsForModeration", summary = "Lấy hàng chờ kiểm duyệt đánh giá")
    public PageResponse<ModerationReviewDto> listForModeration(
            @RequestParam(required = false) ReviewStatus status,
            @RequestParam(required = false) UUID placeId,
            @RequestParam(defaultValue = "OLDEST_FIRST") String sort,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return reviewService.listForModeration(status, placeId, sort, page, size);
    }

    @PostMapping("/{reviewId}/moderate")
    @Operation(operationId = "moderateReview", summary = "Duyệt, từ chối hoặc ẩn một đánh giá")
    public ReviewDto moderateReview(@PathVariable UUID reviewId,
                                    @AuthenticationPrincipal AuthPrincipal principal,
                                    @Valid @RequestBody ModerateReviewRequest request) {
        return reviewService.moderateReview(reviewId, principal.userId(), request);
    }
}
