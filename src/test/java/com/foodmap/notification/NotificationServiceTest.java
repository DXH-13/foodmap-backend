package com.foodmap.notification;

import com.foodmap.common.BadRequestException;
import com.foodmap.identity.api.IdentityApi;
import com.foodmap.identity.api.UserProfile;
import com.foodmap.identity.api.UserRole;
import com.foodmap.notification.api.NotificationType;
import com.foodmap.notification.api.Platform;
import com.foodmap.notification.application.NotificationService;
import com.foodmap.notification.application.PushSender;
import com.foodmap.notification.domain.Notification;
import com.foodmap.notification.domain.NotificationSettings;
import com.foodmap.notification.domain.PushToken;
import com.foodmap.notification.infrastructure.NotificationRepository;
import com.foodmap.notification.infrastructure.NotificationSettingsRepository;
import com.foodmap.notification.infrastructure.PushTokenRepository;
import com.foodmap.notification.web.dto.NotificationSettingsDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test {@link NotificationService} — mock repository và {@link PushSender},
 * không cần CSDL.
 */
class NotificationServiceTest {

    private final NotificationRepository notifications = mock(NotificationRepository.class);
    private final PushTokenRepository pushTokens = mock(PushTokenRepository.class);
    private final NotificationSettingsRepository settings = mock(NotificationSettingsRepository.class);
    private final IdentityApi identity = mock(IdentityApi.class);
    private final PushSender pushSender = mock(PushSender.class);
    private final MessageSource messages = mock(MessageSource.class);

    private final NotificationService service = new NotificationService(
            notifications, pushTokens, settings, identity, pushSender, messages);

    private static UserProfile profile(UUID id) {
        return new UserProfile(id, "an@example.com", "An", null,
                UserRole.USER, "vi", true, Instant.now());
    }

    private void stubLocale(UUID userId) {
        when(identity.getUser(userId)).thenReturn(profile(userId));
        // any(Object[].class) không khớp khi args là null, nên dùng any() thô.
        when(messages.getMessage(anyString(), any(), any(Locale.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("notify: tạo thông báo trong app khi loại được phép (mặc định tất cả)")
    void notifyCreatesInAppNotificationByDefault() {
        UUID userId = UUID.randomUUID();
        when(settings.findById(userId)).thenReturn(Optional.empty());
        when(pushTokens.findByUserId(userId)).thenReturn(List.of());
        when(notifications.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.notify(userId, NotificationType.REVIEW_APPROVED,
                "notification.review_approved.title", Map.of("reviewId", UUID.randomUUID().toString()));

        verify(notifications).save(any(Notification.class));
        verify(pushSender, never()).sendPush(any(), any(), anyString(), any());
    }

    @Test
    @DisplayName("notify: loại đã tắt trong cài đặt thì không tạo thông báo nào")
    void notifySkippedWhenTypeDisabled() {
        UUID userId = UUID.randomUUID();
        when(settings.findById(userId)).thenReturn(Optional.of(
                new NotificationSettings(userId, List.of(NotificationType.REVIEW_REJECTED))));

        service.notify(userId, NotificationType.REVIEW_APPROVED,
                "notification.review_approved.title", Map.of());

        verify(notifications, never()).save(any(Notification.class));
    }

    @Test
    @DisplayName("notify: gửi push thành công thì đánh dấu push_sent_at")
    void notifyMarksPushSentWhenSenderSucceeds() {
        UUID userId = UUID.randomUUID();
        stubLocale(userId);
        when(settings.findById(userId)).thenReturn(Optional.empty());
        when(pushTokens.findByUserId(userId))
                .thenReturn(List.of(new PushToken(userId, "token-1", Platform.ANDROID)));
        when(notifications.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(pushSender.sendPush(eq(userId), eq(NotificationType.SYSTEM_ANNOUNCEMENT),
                anyString(), any())).thenReturn(true);

        service.notify(userId, NotificationType.SYSTEM_ANNOUNCEMENT,
                "notification.welcome.title", Map.of());

        verify(pushSender).sendPush(eq(userId), eq(NotificationType.SYSTEM_ANNOUNCEMENT),
                anyString(), any());
        // save lần 1 tạo thông báo, lần 2 lưu lại sau khi đánh dấu đã gửi push.
        verify(notifications, org.mockito.Mockito.times(2)).save(any(Notification.class));
    }

    @Test
    @DisplayName("notify: gửi push lỗi cũng không làm mất thông báo trong app")
    void notifyKeepsInAppNotificationWhenPushFails() {
        UUID userId = UUID.randomUUID();
        stubLocale(userId);
        when(settings.findById(userId)).thenReturn(Optional.empty());
        when(pushTokens.findByUserId(userId))
                .thenReturn(List.of(new PushToken(userId, "token-1", Platform.IOS)));
        when(notifications.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(pushSender.sendPush(any(), any(), anyString(), any()))
                .thenThrow(new RuntimeException("FCM down"));

        service.notify(userId, NotificationType.SYSTEM_ANNOUNCEMENT,
                "notification.welcome.title", Map.of());

        verify(notifications, org.mockito.Mockito.times(1)).save(any(Notification.class));
    }

    @Test
    @DisplayName("Danh sách: trả đúng trang, dịch tiêu đề, kèm số chưa đọc")
    void listNotificationsReturnsPageAndUnreadCount() {
        UUID userId = UUID.randomUUID();
        stubLocale(userId);
        Notification notification = new Notification(userId, NotificationType.REVIEW_APPROVED,
                "notification.review_approved.title", Map.of());
        when(notifications.findByUserIdOrderByCreatedAtDesc(eq(userId), eq(PageRequest.of(0, 20))))
                .thenReturn(new PageImpl<>(List.of(notification), PageRequest.of(0, 20), 1));
        when(notifications.countByUserIdAndReadFalse(userId)).thenReturn(1L);

        var result = service.listNotifications(userId, false, 0, 20);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).title()).isEqualTo("notification.review_approved.title");
        assertThat(result.unreadCount()).isEqualTo(1L);
        assertThat(result.totalElements()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Danh sách: page/size sai thì 400")
    void listNotificationsRejectsBadPage() {
        UUID userId = UUID.randomUUID();

        assertThatThrownBy(() -> service.listNotifications(userId, false, -1, 20))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.listNotifications(userId, false, 0, 101))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Đánh dấu đã đọc: không truyền id thì đánh dấu tất cả")
    void markAllReadWhenNoIds() {
        UUID userId = UUID.randomUUID();

        service.markNotificationsRead(userId, null);
        service.markNotificationsRead(userId, List.of());

        verify(notifications, org.mockito.Mockito.times(2)).markAllRead(userId);
        verify(notifications, never()).markReadByIds(any(), any());
    }

    @Test
    @DisplayName("Đánh dấu đã đọc: có id thì chỉ đánh dấu những id đó")
    void markReadByIds() {
        UUID userId = UUID.randomUUID();
        List<UUID> ids = List.of(UUID.randomUUID(), UUID.randomUUID());

        service.markNotificationsRead(userId, ids);

        verify(notifications).markReadByIds(userId, ids);
        verify(notifications, never()).markAllRead(any());
    }

    @Test
    @DisplayName("Cài đặt: chưa từng đặt thì mặc định cho phép tất cả các loại")
    void getSettingsDefaultsToAllTypes() {
        UUID userId = UUID.randomUUID();
        when(settings.findById(userId)).thenReturn(Optional.empty());

        NotificationSettingsDto result = service.getSettings(userId);

        assertThat(result.enabledTypes()).containsExactlyInAnyOrder(NotificationType.values());
    }

    @Test
    @DisplayName("Cài đặt: cập nhật ghi đè toàn bộ danh sách")
    void updateSettingsOverwrites() {
        UUID userId = UUID.randomUUID();
        when(settings.findById(userId)).thenReturn(Optional.empty());
        when(settings.save(any(NotificationSettings.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        NotificationSettingsDto result = service.updateSettings(
                userId, List.of(NotificationType.REVIEW_APPROVED));

        assertThat(result.enabledTypes()).containsExactly(NotificationType.REVIEW_APPROVED);
        verify(settings).save(any(NotificationSettings.class));
    }

    @Test
    @DisplayName("Đăng ký push token: đăng ký lại cùng token chỉ chạm last_used_at")
    void registerPushTokenIsIdempotent() {
        UUID userId = UUID.randomUUID();
        PushToken existing = new PushToken(userId, "token-1", Platform.ANDROID);
        when(pushTokens.findByUserIdAndTokenAndPlatform(userId, "token-1", Platform.ANDROID))
                .thenReturn(Optional.of(existing));

        service.registerPushToken(userId, "token-1", Platform.ANDROID);

        // Chỉ lưu lại bản ghi cũ (chạm last_used_at), không tạo bản ghi mới.
        verify(pushTokens, org.mockito.Mockito.times(1)).save(existing);
    }

    @Test
    @DisplayName("Đăng ký push token: token mới thì tạo bản ghi")
    void registerPushTokenCreatesNew() {
        UUID userId = UUID.randomUUID();
        when(pushTokens.findByUserIdAndTokenAndPlatform(userId, "token-1", Platform.ANDROID))
                .thenReturn(Optional.empty());

        service.registerPushToken(userId, "token-1", Platform.ANDROID);

        verify(pushTokens).save(any(PushToken.class));
    }
}
