-- FoodMap — Bước 6: tách schema theo module.
--
-- Chuyển 4 bảng của module identity từ schema public sang schema identity.
-- Dữ liệu giữ nguyên: ALTER TABLE ... SET SCHEMA chỉ đổi vị trí của bảng,
-- không chép hay xoá dòng nào; index và ràng buộc đi cùng bảng.
--
-- Không có FK chéo schema trong nhóm này: cả 3 bảng token đều tham chiếu
-- users trong cùng schema identity, nên các FK giữ nguyên và tự trỏ đúng
-- sau khi chuyển.
--
-- Entity tương ứng: @Table(name = "...", schema = "identity").

CREATE SCHEMA IF NOT EXISTS identity;

ALTER TABLE public.users SET SCHEMA identity;
ALTER TABLE public.refresh_tokens SET SCHEMA identity;
ALTER TABLE public.password_reset_tokens SET SCHEMA identity;
ALTER TABLE public.email_verification_tokens SET SCHEMA identity;
