-- FoodMap — Bước 6: tách schema theo module.
--
-- Chuyển bảng visits của module visit từ schema public sang schema visit.
-- Dữ liệu giữ nguyên (xem V4).
--
-- FK chéo schema phải bỏ (kiến trúc cấm FK chéo schema, xem V5):
--   visits.user_id  -> identity.users
--   visits.place_id -> place.places
--
-- Entity tương ứng: @Table(name = "visits", schema = "visit").

CREATE SCHEMA IF NOT EXISTS visit;

ALTER TABLE public.visits DROP CONSTRAINT visits_user_id_fkey;
ALTER TABLE public.visits DROP CONSTRAINT visits_place_id_fkey;

ALTER TABLE public.visits SET SCHEMA visit;
