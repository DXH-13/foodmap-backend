-- FoodMap — Bước 6: tách schema theo module.
--
-- Chuyển 2 bảng của module chatbot từ schema public sang schema chatbot.
-- Dữ liệu giữ nguyên (xem V4).
--
-- FK chéo schema phải bỏ (kiến trúc cấm FK chéo schema, xem V5):
--   chat_sessions.user_id -> identity.users
-- FK nội bộ chat_messages.chat_session_id -> chat_sessions giữ nguyên
-- (cùng schema chatbot).
--
-- Entity tương ứng: @Table(name = "...", schema = "chatbot").

CREATE SCHEMA IF NOT EXISTS chatbot;

ALTER TABLE public.chat_sessions DROP CONSTRAINT chat_sessions_user_id_fkey;

ALTER TABLE public.chat_sessions SET SCHEMA chatbot;
ALTER TABLE public.chat_messages SET SCHEMA chatbot;
