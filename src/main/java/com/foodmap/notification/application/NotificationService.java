package com.foodmap.notification.application;

import com.foodmap.common.BadRequestException;
import com.foodmap.identity.api.IdentityApi;
import com.foodmap.notification.api.NotificationApi;
import com.foodmap.notification.api.NotificationType;
import com.foodmap.notification.api.Platform;
import com.foodmap.notification.domain.Notification;
import com.foodmap.notification.domain.NotificationSettings;
import com.foodmap.notification.domain.PushToken;
import com.foodmap.notification.infrastructure.NotificationRepository;
import com.foodmap.notification.infrastructure.NotificationSettingsRepository;
import com.foodmap.notification.infrastructure.PushTokenRepository;
import com.foodmap.notification.web.dto.NotificationDto;
import com.foodmap.notification.web.dto.NotificationListResponse;
import com.foodmap.notification.web.dto.NotificationSettingsDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Use case thông báo (Phase 4): liệt kê, đánh dấu đã đọc, cài đặt nhận thông
 * báo, đăng ký push token, và tạo thông báo cho module khác nhờ.
 *
 * <p>Quy tắc cài đặt ({@code notification_settings.enabled_types}):
 * <ul>
 *   <li>Chưa từng đặt (không có hàng) → cho phép tất cả các loại.</li>
 *   <li>Đã đặt → danh sách đó là allowlist: loại nào không có trong danh sách
 *       thì không tạo thông báo (cả trong app lẫn push). Rỗng nghĩa là tắt hết.</li>
 * </ul>
 * Hợp đồng mô tả trường này là "các loại được phép gửi push"; cách đọc ở đây
 * là nó chặn cả việc tạo thông báo — nếu sản phẩm muốn thông báo trong app vẫn
 * hiện dù đã tắt push thì chỉ cần sửa riêng {@link #notify} (tạo trong app
 * trước, kiểm tra cài đặt chỉ lúc đẩy push).
 */
@Service
public class NotificationService implements NotificationApi {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notifications;
    private final PushTokenRepository pushTokens;
    private final NotificationSettingsRepository settings;
    private final IdentityApi identity;
    private final PushSender pushSender;
    private final MessageSource messages;

    public NotificationService(NotificationRepository notifications,
                               PushTokenRepository pushTokens,
                               NotificationSettingsRepository settings,
                               IdentityApi identity,
                               PushSender pushSender,
                               MessageSource messages) {
        this.notifications = notifications;
        this.pushTokens = pushTokens;
        this.settings = settings;
        this.identity = identity;
        this.pushSender = pushSender;
        this.messages = messages;
    }

    // ── API công khai cho module khác (NotificationApi) ─────────────────────

    /**
     * Tạo thông báo trong app rồi thử đẩy push. Im lặng bỏ qua khi người dùng
     * đã tắt loại này trong cài đặt — đó là ý muốn của họ, không phải lỗi.
     * Push lỗi không bao giờ làm hỏng việc tạo thông báo trong app.
     */
    @Override
    @Transactional
    public void notify(UUID userId, NotificationType type, String titleKey,
                       Map<String, Object> payload) {
        if (!isTypeEnabled(userId, type)) {
            log.debug("Bỏ qua thông báo type={} cho userId={} — người dùng đã tắt loại này.", type, userId);
            return;
        }
        Notification notification = new Notification(userId, type, titleKey, payload);
        notifications.save(notification);
        if (!pushTokens.findByUserId(userId).isEmpty()) {
            String title = resolveTitle(notification, recipientLocale(userId));
            boolean sent = false;
            try {
                sent = pushSender.sendPush(userId, type, title, notification.getPayload());
            } catch (RuntimeException e) {
                // Cổng push hứa không ném lỗi, nhưng lỡ bản thật có ném thì
                // thông báo trong app vẫn phải còn — chỉ ghi log.
                log.warn("Gửi push thất bại cho userId={} type={} (thông báo trong app vẫn giữ).",
                        userId, type, e.toString());
            }
            if (sent) {
                notification.markPushSent(Instant.now());
                notifications.save(notification);
            }
        }
    }

    /** Loại này có được phép gửi cho người dùng không (mặc định: cho phép tất cả). */
    @Transactional(readOnly = true)
    public boolean isTypeEnabled(UUID userId, NotificationType type) {
        return settings.findById(userId)
                .map(row -> row.getEnabledTypes().contains(type))
                .orElse(true);
    }

    // ── Endpoint cho chính mình ─────────────────────────────────────────────

    /** Danh sách thông báo của chính mình, mới nhất trước, kèm số chưa đọc. */
    @Transactional(readOnly = true)
    public NotificationListResponse listNotifications(UUID userId, boolean unreadOnly,
                                                      int page, int size) {
        validatePage(page, size);
        Pageable pageable = PageRequest.of(page, size);
        Page<Notification> result = unreadOnly
                ? notifications.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId, pageable)
                : notifications.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        Locale locale = recipientLocale(userId);
        List<NotificationDto> content = result.getContent().stream()
                .map(notification -> toDto(notification, locale))
                .toList();
        return new NotificationListResponse(
                content,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                notifications.countByUserIdAndReadFalse(userId));
    }

    /**
     * Đánh dấu đã đọc. Danh sách rỗng hoặc không truyền nghĩa là tất cả —
     * theo mô tả của operation {@code markNotificationsRead} trong hợp đồng.
     * Chỉ chạm thông báo thuộc về người gọi.
     */
    @Transactional
    public void markNotificationsRead(UUID userId, List<UUID> notificationIds) {
        if (notificationIds == null || notificationIds.isEmpty()) {
            notifications.markAllRead(userId);
        } else {
            notifications.markReadByIds(userId, notificationIds);
        }
    }

    /** Cài đặt hiện tại — chưa từng đặt thì trả đầy đủ các loại (mặc định). */
    @Transactional(readOnly = true)
    public NotificationSettingsDto getSettings(UUID userId) {
        List<NotificationType> enabled = settings.findById(userId)
                .map(NotificationSettings::getEnabledTypes)
                .orElse(List.of(NotificationType.values()));
        return new NotificationSettingsDto(enabled);
    }

    /** Ghi đè toàn bộ danh sách loại được phép — client luôn gửi danh sách đầy đủ. */
    @Transactional
    public NotificationSettingsDto updateSettings(UUID userId, List<NotificationType> enabledTypes) {
        if (enabledTypes == null) {
            throw new BadRequestException("VALIDATION_FAILED", "common.error.validation");
        }
        NotificationSettings row = settings.findById(userId)
                .orElseGet(() -> new NotificationSettings(userId, enabledTypes));
        row.setEnabledTypes(enabledTypes);
        settings.save(row);
        return new NotificationSettingsDto(row.getEnabledTypes());
    }

    /**
     * Đăng ký push token của thiết bị. Idempotent: đăng ký lại cùng token thì
     * chỉ cập nhật {@code last_used_at}, không tạo bản ghi mới.
     */
    @Transactional
    public void registerPushToken(UUID userId, String token, Platform platform) {
        pushTokens.findByUserIdAndTokenAndPlatform(userId, token, platform)
                .ifPresentOrElse(
                        existing -> {
                            existing.touch();
                            pushTokens.save(existing);
                        },
                        () -> pushTokens.save(new PushToken(userId, token, platform)));
    }

    // ── Hỗ trợ nội bộ ───────────────────────────────────────────────────────

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new BadRequestException("VALIDATION_FAILED", "common.error.validation");
        }
    }

    private Locale recipientLocale(UUID userId) {
        try {
            String preferred = identity.getUser(userId).preferredLocale();
            if (preferred != null && !preferred.isBlank()) {
                return Locale.forLanguageTag(preferred);
            }
        } catch (RuntimeException e) {
            // Người dùng đã xoá mềm sau khi thông báo được tạo — vẫn trả danh
            // sách được, dùng ngôn ngữ của request hiện tại.
            log.debug("Không lấy được ngôn ngữ của userId={} ({}), dùng locale mặc định.", userId, e.toString());
        }
        return LocaleContextHolder.getLocale();
    }

    /**
     * Dịch tiêu đề từ {@code titleKey} theo ngôn ngữ người nhận. Tham số dịch
     * lấy từ payload — hiện chỉ có {@code placeName} cho thông báo "quán có
     * đánh giá mới". Thiếu key thì trả chính key đó để endpoint không 500 chỉ
     * vì một thông báo cũ thiếu bản dịch.
     */
    private String resolveTitle(Notification notification, Locale locale) {
        Object placeName = notification.getPayload().get("placeName");
        Object[] args = placeName == null ? null : new Object[]{placeName};
        try {
            return messages.getMessage(notification.getTitleKey(), args, locale);
        } catch (RuntimeException e) {
            log.warn("Thiếu bản dịch cho key {} (locale {}), trả key thay cho tiêu đề.",
                    notification.getTitleKey(), locale, e);
            return notification.getTitleKey();
        }
    }

    private NotificationDto toDto(Notification notification, Locale locale) {
        Map<String, Object> payload = notification.getPayload();
        return new NotificationDto(
                notification.getId(),
                notification.getType(),
                resolveTitle(notification, locale),
                null,
                toUuid(payload.get("placeId")),
                toUuid(payload.get("reviewId")),
                notification.isRead(),
                notification.getCreatedAt());
    }

    private static UUID toUuid(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof UUID uuid) {
            return uuid;
        }
        try {
            return UUID.fromString(value.toString());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
