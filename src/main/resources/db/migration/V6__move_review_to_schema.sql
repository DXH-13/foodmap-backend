-- FoodMap — Bước 6: tách schema theo module.
--
-- Chuyển 2 bảng của module review từ schema public sang schema review.
-- Dữ liệu giữ nguyên (xem V4).
--
-- FK chéo schema phải bỏ (kiến trúc cấm FK chéo schema, xem V5):
--   reviews.place_id    -> place.places
--   reviews.user_id     -> identity.users
--   reviews.moderated_by -> identity.users
--   review_media.uploaded_by -> identity.users
-- FK nội bộ review_media.review_id -> reviews giữ nguyên (cùng schema review).
--
-- Entity tương ứng: @Table(name = "...", schema = "review").

CREATE SCHEMA IF NOT EXISTS review;

ALTER TABLE public.reviews DROP CONSTRAINT reviews_place_id_fkey;
ALTER TABLE public.reviews DROP CONSTRAINT reviews_user_id_fkey;
ALTER TABLE public.reviews DROP CONSTRAINT reviews_moderated_by_fkey;
ALTER TABLE public.review_media DROP CONSTRAINT review_media_uploaded_by_fkey;

ALTER TABLE public.reviews SET SCHEMA review;
ALTER TABLE public.review_media SET SCHEMA review;
