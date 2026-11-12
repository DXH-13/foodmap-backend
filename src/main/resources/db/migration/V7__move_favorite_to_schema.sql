-- FoodMap — Bước 6: tách schema theo module.
--
-- Chuyển bảng favorites của module favorite từ schema public sang schema favorite.
-- Dữ liệu giữ nguyên (xem V4).
--
-- FK chéo schema phải bỏ (kiến trúc cấm FK chéo schema, xem V5):
--   favorites.user_id  -> identity.users
--   favorites.place_id -> place.places
--
-- Entity tương ứng: @Table(name = "favorites", schema = "favorite").

CREATE SCHEMA IF NOT EXISTS favorite;

ALTER TABLE public.favorites DROP CONSTRAINT favorites_user_id_fkey;
ALTER TABLE public.favorites DROP CONSTRAINT favorites_place_id_fkey;

ALTER TABLE public.favorites SET SCHEMA favorite;
