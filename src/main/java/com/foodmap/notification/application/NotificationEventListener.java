package com.foodmap.notification.application;

import com.foodmap.common.NotFoundException;
import com.foodmap.identity.api.UserRegistered;
import com.foodmap.notification.api.NotificationApi;
import com.foodmap.notification.api.NotificationType;
import com.foodmap.place.api.PlaceApi;
import com.foodmap.review.api.ReviewApi;
import com.foodmap.review.api.ReviewCreated;
import com.foodmap.review.api.ReviewModerated;
import com.foodmap.review.api.ReviewStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Nghe sự kiện của module khác để tạo thông báo trong app (Phase 4).
 *
 * <p>Listener đặt ở module lá {@code notification} nên không tạo vòng tròn
 * Modulith: {@code notification} không có module nào gọi vào, và nó chỉ gọi
 * ra qua {@code api/} công khai của {@code identity}/{@code place}/{@code review}.
 * Chạy sau commit (nhờ {@code @ApplicationModuleListener}) trong transaction
 * riêng — nếu lỗi thì bản ghi outbox ở lại và được thử lại, sự kiện gốc không mất.
 *
 * <p><b>Ánh xạ loại thông báo</b> — enum {@code NotificationType} trong hợp đồng
 * và CHECK trên bảng {@code notifications} chỉ có 6 giá trị, không có
 * {@code WELCOME} hay {@code NEW_REVIEW}, nên hai thông báo đó dùng loại chung
 * {@link NotificationType#SYSTEM_ANNOUNCEMENT} và phân biệt bằng
 * {@code titleKey}. Muốn loại riêng thì phải đổi cả hợp đồng lẫn CHECK ở
 * migration mới — việc của bước sau.
 *
 * <p><b>Không nghe:</b>
 * <ul>
 *   <li>{@code VisitRecorded} — bảng sự kiện trong tài liệu kiến trúc giao nó
 *       cho {@code user} (gợi ý) và {@code review} (nhắc đánh giá), không giao
 *       cho {@code notification}; cũng không có loại thông báo nào khớp với
 *       "hãy đánh giá quán vừa ghé". Khi nào sản phẩm định nghĩa loại đó thì
 *       thêm listener sau.</li>
 *   <li>{@code ReviewUpdated} / {@code ReviewDeleted} — sửa/xoá đánh giá của
 *       chính mình không phải việc cần báo cho ai.</li>
 *   <li>{@code PlaceCreated} / {@code PlaceUpdated} — loại {@code PLACE_UPDATED}
 *       và {@code NEW_PLACE_NEARBY} trong enum hiện chưa gắn với tiêu chí người
 *       nhận nào (theo dõi quán? ở gần?) nên chưa tạo thông báo để tránh spam.
 *       Khi có tính năng theo dõi quán thì thêm listener theo tiêu chí đó.</li>
 * </ul>
 */
@Component
public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

    private final NotificationApi notificationApi;
    private final ReviewApi reviews;
    private final PlaceApi places;

    public NotificationEventListener(NotificationApi notificationApi,
                                     ReviewApi reviews,
                                     PlaceApi places) {
        this.notificationApi = notificationApi;
        this.reviews = reviews;
        this.places = places;
    }

    /** Tài khoản mới → chào mừng trong app. */
    @ApplicationModuleListener
    public void on(UserRegistered event) {
        notificationApi.notify(event.userId(), NotificationType.SYSTEM_ANNOUNCEMENT,
                "notification.welcome.title", Map.of());
    }

    /**
     * Đánh giá mới → báo chủ quán (người tạo địa điểm), trừ khi quán không có
     * người tạo, hoặc chính tác giả là chủ quán (tự đánh giá quán của mình thì
     * không cần báo cho chính mình).
     */
    @ApplicationModuleListener
    public void on(ReviewCreated event) {
        Optional<ReviewApi.ReviewDetails> review = reviews.findReview(event.reviewId());
        if (review.isEmpty()) {
            log.debug("Bỏ qua ReviewCreated reviewId={} — đánh giá đã bị xoá.", event.reviewId());
            return;
        }
        UUID authorId = review.get().authorId();
        Optional<UUID> creator = places.findCreatorById(event.placeId());
        if (creator.isEmpty()) {
            log.debug("Bỏ qua ReviewCreated placeId={} — quán không có người tạo.", event.placeId());
            return;
        }
        if (creator.get().equals(authorId)) {
            return;
        }
        String placeName;
        try {
            placeName = places.getPlace(event.placeId()).name();
        } catch (NotFoundException e) {
            log.debug("Bỏ qua ReviewCreated placeId={} — quán đã bị xoá.", event.placeId());
            return;
        }
        notificationApi.notify(creator.get(), NotificationType.SYSTEM_ANNOUNCEMENT,
                "notification.new_review.title",
                Map.of("placeId", event.placeId().toString(),
                        "reviewId", event.reviewId().toString(),
                        "placeName", placeName));
    }

    /**
     * Kết quả kiểm duyệt → báo tác giả. Chỉ hai kết quả có loại trong enum:
     * duyệt ({@code REVIEW_APPROVED}) và từ chối ({@code REVIEW_REJECTED}).
     * Ẩn / bỏ ẩn là thao tác hiển thị của admin, không có loại thông báo khớp
     * nên không tạo thông báo (bỏ ẩn thì trạng thái trong event cũng là
     * {@code APPROVED}, xem {@code ReviewService.moderateReview}).
     */
    @ApplicationModuleListener
    public void on(ReviewModerated event) {
        Optional<ReviewApi.ReviewDetails> review = reviews.findReview(event.reviewId());
        if (review.isEmpty()) {
            log.debug("Bỏ qua ReviewModerated reviewId={} — đánh giá đã bị xoá.", event.reviewId());
            return;
        }
        UUID authorId = review.get().authorId();
        Map<String, Object> payload = Map.of(
                "placeId", event.placeId().toString(),
                "reviewId", event.reviewId().toString());
        if (event.status() == ReviewStatus.APPROVED) {
            notificationApi.notify(authorId, NotificationType.REVIEW_APPROVED,
                    "notification.review_approved.title", payload);
        } else if (event.status() == ReviewStatus.REJECTED) {
            notificationApi.notify(authorId, NotificationType.REVIEW_REJECTED,
                    "notification.review_rejected.title", payload);
        }
        // HIDDEN: không có loại thông báo khớp — im lặng bỏ qua.
    }
}
