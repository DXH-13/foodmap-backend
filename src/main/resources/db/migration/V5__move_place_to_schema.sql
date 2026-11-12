-- FoodMap — Bước 6: tách schema theo module.
--
-- Chuyển 7 bảng của module place từ schema public sang schema place.
-- Dữ liệu giữ nguyên (xem V4).
--
-- FK chéo schema phải bỏ, vì kiến trúc cấm FK chéo schema — toàn vẹn tham
-- chiếu giữa các module giữ bằng code (gọi qua api/), không giữ bằng CSDL:
--   places.created_by -> identity.users
-- Các FK nội bộ (place_translations/category_translations/place_categories/
-- opening_hours/place_media -> places hoặc categories) giữ nguyên vì cùng
-- chuyển sang schema place.
--
-- Index đi cùng bảng, không cần làm gì thêm — kể cả index GiST
-- idx_places_location và index GIN idx_place_translations_name (dùng hàm
-- public.f_unaccent, hàm này ở lại schema public).
--
-- PostGIS và hàm f_unaccent ở lại schema public: chúng dùng chung cho mọi
-- module, không thuộc riêng module nào (xem V1).
--
-- Entity tương ứng: @Table(name = "...", schema = "place").
-- Truy vấn native trong PlaceRepository đã ghi rõ place.places và
-- place.place_translations.

CREATE SCHEMA IF NOT EXISTS place;

ALTER TABLE public.places DROP CONSTRAINT places_created_by_fkey;

ALTER TABLE public.places SET SCHEMA place;
ALTER TABLE public.place_translations SET SCHEMA place;
ALTER TABLE public.categories SET SCHEMA place;
ALTER TABLE public.category_translations SET SCHEMA place;
ALTER TABLE public.place_categories SET SCHEMA place;
ALTER TABLE public.opening_hours SET SCHEMA place;
ALTER TABLE public.place_media SET SCHEMA place;
