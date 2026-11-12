-- FoodMap — Bước 6: tách schema theo module.
--
-- feedbacks và audit_logs CHƯA có module/entity sở hữu (thuộc các module
-- content, moderation, audit của phase sau theo tài liệu kiến trúc) nên
-- hai bảng này Ở LẠI schema public. Tuy nhiên các FK của chúng trỏ sang
-- bảng đã chuyển sang schema khác — kiến trúc cấm FK chéo schema nên phải
-- bỏ các FK này:
--   feedbacks.place_id            -> place.places
--   feedbacks.user_id             -> identity.users
--   feedbacks.duplicate_of_place_id -> place.places
--   feedbacks.resolved_by         -> identity.users
--   audit_logs.actor_id           -> identity.users
--
-- Bỏ FK không đổi hành vi chạy: hiện chưa có code nào ghi vào hai bảng này
-- (chưa có module sở hữu), và không có truy vấn nào dựa vào cascade của
-- các FK này. Khi module sở hữu ra đời, toàn vẹn tham chiếu giữ bằng code
-- (gọi qua api/) như các module khác, và lúc đó hai bảng sẽ chuyển sang
-- schema của module sở hữu bằng một migration riêng.

ALTER TABLE public.feedbacks DROP CONSTRAINT feedbacks_place_id_fkey;
ALTER TABLE public.feedbacks DROP CONSTRAINT feedbacks_user_id_fkey;
ALTER TABLE public.feedbacks DROP CONSTRAINT feedbacks_duplicate_of_place_id_fkey;
ALTER TABLE public.feedbacks DROP CONSTRAINT feedbacks_resolved_by_fkey;
ALTER TABLE public.audit_logs DROP CONSTRAINT audit_logs_actor_id_fkey;
