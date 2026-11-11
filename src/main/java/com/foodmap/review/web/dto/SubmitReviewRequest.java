package com.foodmap.review.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Gửi hoặc cập nhật đánh giá. Khớp schema {@code SubmitReviewRequest} trong hợp đồng.
 *
 * <p>{@code mediaIds}: hợp đồng cho đính kèm tối đa 5 ảnh và 1 video, nhưng
 * luồng upload media (presigned URL + confirm) thuộc bước sau nên hiện tại
 * backend chưa có media nào để gắn — field được nhận và bỏ qua, danh sách media
 * trả ra luôn rỗng. Ghi rõ để không ai tưởng đã gắn được ảnh.
 */
public record SubmitReviewRequest(
        @NotNull @Min(1) @Max(5) Integer rating,
        String content,
        List<UUID> mediaIds
) {
}
