-- FoodMap — Bước 6: tách schema theo module.
--
-- Chuyển 3 bảng của module notification từ schema public sang schema notification.
-- Dữ liệu giữ nguyên (xem V4).
--
-- FK chéo schema phải bỏ (kiến trúc cấm FK chéo schema, xem V5):
--   notifications.user_id         -> identity.users
--   push_tokens.user_id           -> identity.users
--   notification_settings.user_id -> identity.users
--
-- Entity tương ứng: @Table(name = "...", schema = "notification").

CREATE SCHEMA IF NOT EXISTS notification;

ALTER TABLE public.notifications DROP CONSTRAINT notifications_user_id_fkey;
ALTER TABLE public.push_tokens DROP CONSTRAINT push_tokens_user_id_fkey;
ALTER TABLE public.notification_settings DROP CONSTRAINT notification_settings_user_id_fkey;

ALTER TABLE public.notifications SET SCHEMA notification;
ALTER TABLE public.push_tokens SET SCHEMA notification;
ALTER TABLE public.notification_settings SET SCHEMA notification;
