-- Đánh giá, yêu thích và lượt đến MẪU cho môi trường local. KHÔNG chạy ở dev hay production.
--
-- Bốn người dùng mẫu mới, mật khẩu chung Foodmap@123 (cùng hash với V901).
-- Không có ảnh: MinIO local chưa có file nào, nên review_media để trống.
-- Không đánh giá địa điểm DRAFT (11111111-...-105).

INSERT INTO users (id, email, password_hash, display_name, role, preferred_locale, email_verified, email_verified_at)
VALUES
    ('22222222-2222-2222-2222-222222222204', 'lan@foodmap.local',
     '$2a$12$HfCJA7RumERkGjY7SnEhjeMmH7Pu3VuPZ/MqNugXbyReX.WliC6pC',
     'Ngọc Lan', 'USER', 'vi', TRUE, now()),
    ('22222222-2222-2222-2222-222222222205', 'minh@foodmap.local',
     '$2a$12$HfCJA7RumERkGjY7SnEhjeMmH7Pu3VuPZ/MqNugXbyReX.WliC6pC',
     'Minh Trần', 'USER', 'vi', TRUE, now()),
    ('22222222-2222-2222-2222-222222222206', 'thao@foodmap.local',
     '$2a$12$HfCJA7RumERkGjY7SnEhjeMmH7Pu3VuPZ/MqNugXbyReX.WliC6pC',
     'Phương Thảo', 'USER', 'vi', TRUE, now()),
    ('22222222-2222-2222-2222-222222222207', 'david@foodmap.local',
     '$2a$12$HfCJA7RumERkGjY7SnEhjeMmH7Pu3VuPZ/MqNugXbyReX.WliC6pC',
     'David Nguyễn', 'USER', 'en', TRUE, now());

-- Một người một đánh giá mỗi địa điểm (uq_reviews_user_place).
INSERT INTO reviews (place_id, user_id, rating, content, locale, status, moderation_note,
                     moderated_by, moderated_at, created_at, updated_at)
VALUES
    -- Phở Hoà Pasteur
    ('11111111-1111-1111-1111-111111111101', '22222222-2222-2222-2222-222222222204', 5,
     'Nước dùng trong mà đậm vị, thịt bò tái mềm. Sáng chủ nhật hơi đông nhưng lên món nhanh.',
     'vi', 'APPROVED', NULL, '22222222-2222-2222-2222-222222222202', now() - interval '20 days',
     now() - interval '21 days', now() - interval '21 days'),
    ('11111111-1111-1111-1111-111111111101', '22222222-2222-2222-2222-222222222205', 4,
     'Phở ngon, rau thơm tươi. Giá hơi cao so với mặt bằng chung nhưng xứng đáng.',
     'vi', 'APPROVED', NULL, '22222222-2222-2222-2222-222222222202', now() - interval '14 days',
     now() - interval '15 days', now() - interval '15 days'),
    ('11111111-1111-1111-1111-111111111101', '22222222-2222-2222-2222-222222222203', 4,
     'Ăn sáng ở đây mấy lần rồi, lần nào cũng ổn. Quẩy giòn, chấm nước phở là hết sảy.',
     'vi', 'APPROVED', NULL, '22222222-2222-2222-2222-222222222202', now() - interval '5 days',
     now() - interval '6 days', now() - interval '6 days'),

    -- Bánh mì Huỳnh Hoa
    ('11111111-1111-1111-1111-111111111102', '22222222-2222-2222-2222-222222222206', 5,
     'Ổ bánh mì to, pate béo, chả lụa nhiều. Một ổ ăn no tới tối.',
     'vi', 'APPROVED', NULL, '22222222-2222-2222-2222-222222222202', now() - interval '12 days',
     now() - interval '13 days', now() - interval '13 days'),
    ('11111111-1111-1111-1111-111111111102', '22222222-2222-2222-2222-222222222207', 3,
     'Nhân đầy đặn nhưng hơi mặn với mình. Xếp hàng gần 20 phút.',
     'vi', 'APPROVED', NULL, '22222222-2222-2222-2222-222222222202', now() - interval '9 days',
     now() - interval '10 days', now() - interval '10 days'),

    -- Khu ăn uống chợ Bến Thành
    ('11111111-1111-1111-1111-111111111103', '22222222-2222-2222-2222-222222222205', 3,
     'Nhiều món để chọn, tiện cho khách du lịch. Giá trong chợ cao hơn bên ngoài khá nhiều.',
     'vi', 'APPROVED', NULL, '22222222-2222-2222-2222-222222222202', now() - interval '8 days',
     now() - interval '8 days', now() - interval '8 days'),
    ('11111111-1111-1111-1111-111111111103', '22222222-2222-2222-2222-222222222204', 4,
     'Chè và bánh bèo ở quầy góc trong cùng rất ngon. Đi buổi trưa đỡ nóng hơn.',
     'vi', 'APPROVED', NULL, '22222222-2222-2222-2222-222222222202', now() - interval '3 days',
     now() - interval '4 days', now() - interval '4 days'),

    -- Cơm tấm Ba Ghiền (đang tạm đóng cửa nhưng vẫn hiển thị công khai)
    ('11111111-1111-1111-1111-111111111104', '22222222-2222-2222-2222-222222222206', 4,
     'Miếng sườn to bằng bàn tay, ướp vừa, nướng thơm. Cơm tấm dẻo.',
     'vi', 'APPROVED', NULL, '22222222-2222-2222-2222-222222222202', now() - interval '30 days',
     now() - interval '31 days', now() - interval '31 days'),

    -- Bún chả Hương Liên
    ('11111111-1111-1111-1111-111111111201', '22222222-2222-2222-2222-222222222207', 5,
     'Chả nướng than hoa thơm lừng, nước chấm chua ngọt vừa phải. Nhất định phải gọi thêm nem.',
     'vi', 'APPROVED', NULL, '22222222-2222-2222-2222-222222222202', now() - interval '2 days',
     now() - interval '2 days', now() - interval '2 days'),
    ('11111111-1111-1111-1111-111111111201', '22222222-2222-2222-2222-222222222204', 5,
     'Đúng vị bún chả Hà Nội. Phục vụ nhanh dù quán lúc nào cũng kín chỗ.',
     'vi', 'APPROVED', NULL, '22222222-2222-2222-2222-222222222202', now() - interval '11 days',
     now() - interval '11 days', now() - interval '11 days'),
    ('11111111-1111-1111-1111-111111111201', '22222222-2222-2222-2222-222222222205', 4,
     'Ngon, nhưng bàn ghế hơi chật. Nên đi trước 11 giờ.',
     'vi', 'APPROVED', NULL, '22222222-2222-2222-2222-222222222202', now() - interval '18 days',
     now() - interval '18 days', now() - interval '18 days'),

    -- Phở Bát Đàn
    ('11111111-1111-1111-1111-111111111202', '22222222-2222-2222-2222-222222222206', 5,
     'Phở bò chín nước dùng ngọt xương, hành chần ăn kèm rất hợp. Xếp hàng tự bưng nhưng đáng.',
     'vi', 'APPROVED', NULL, '22222222-2222-2222-2222-222222222202', now() - interval '7 days',
     now() - interval '7 days', now() - interval '7 days'),
    ('11111111-1111-1111-1111-111111111202', '22222222-2222-2222-2222-222222222205', 2,
     'Nước dùng hôm mình đi hơi nhạt, phải chờ lâu. Có lẽ hôm đó không may.',
     'vi', 'APPROVED', NULL, '22222222-2222-2222-2222-222222222202', now() - interval '16 days',
     now() - interval '16 days', now() - interval '16 days'),

    -- Của user@foodmap.local: một chờ duyệt, một bị từ chối
    ('11111111-1111-1111-1111-111111111201', '22222222-2222-2222-2222-222222222203', 5,
     'Lần đầu ăn bún chả ở Hà Nội, quá ngon! Sẽ quay lại.',
     'vi', 'PENDING', NULL, NULL, NULL,
     now() - interval '1 day', now() - interval '1 day'),
    ('11111111-1111-1111-1111-111111111102', '22222222-2222-2222-2222-222222222203', 1,
     'Dở tệ, ai ăn là phí tiền!!! Inbox mình để biết quán khác ngon hơn.',
     'vi', 'REJECTED', 'Nội dung mang tính quảng cáo và thiếu mô tả trải nghiệm cụ thể.',
     '22222222-2222-2222-2222-222222222202', now() - interval '2 days',
     now() - interval '3 days', now() - interval '2 days');

-- Điểm trung bình và số đánh giá: CHỈ tính review APPROVED chưa xoá (FR-REVIEW-05).
-- Tính lại cho mọi địa điểm, ghi đè số giả trong V900. Không có đánh giá thì NULL, không phải 0.
UPDATE places p
SET review_count   = s.cnt,
    average_rating = s.avg_rating
FROM (
    SELECT pl.id,
           count(r.id)                     AS cnt,
           ROUND(AVG(r.rating)::numeric, 1) AS avg_rating
    FROM places pl
    LEFT JOIN reviews r ON r.place_id = pl.id AND r.status = 'APPROVED' AND r.deleted_at IS NULL
    GROUP BY pl.id
) s
WHERE s.id = p.id;

-- Yêu thích của user@foodmap.local, thêm vào lúc khác nhau để thử sort RECENTLY_ADDED
INSERT INTO favorites (user_id, place_id, created_at) VALUES
    ('22222222-2222-2222-2222-222222222203', '11111111-1111-1111-1111-111111111101', now() - interval '10 days'),
    ('22222222-2222-2222-2222-222222222203', '11111111-1111-1111-1111-111111111201', now() - interval '3 days'),
    ('22222222-2222-2222-2222-222222222203', '11111111-1111-1111-1111-111111111102', now() - interval '1 day');

-- Lượt đến: vị trí ghi nhận lệch vài chục mét về phía bắc quán, distance_meters tính bằng PostGIS
-- cho khớp đúng với recorded_location (luật 200m — FR-VISIT-02).
INSERT INTO visits (user_id, place_id, visit_date, recorded_location, distance_meters, created_at)
SELECT '22222222-2222-2222-2222-222222222203'::uuid,
       p.id,
       CAST((now() - v.days_ago * interval '1 day') AT TIME ZONE 'Asia/Ho_Chi_Minh' AS date),
       ST_SetSRID(ST_MakePoint(ST_X(p.location::geometry), ST_Y(p.location::geometry) + v.lat_offset), 4326)::geography,
       ROUND(ST_Distance(p.location,
             ST_SetSRID(ST_MakePoint(ST_X(p.location::geometry), ST_Y(p.location::geometry) + v.lat_offset), 4326)::geography))::int,
       now() - v.days_ago * interval '1 day'
FROM (VALUES
    ('11111111-1111-1111-1111-111111111101'::uuid, 6,  0.0003),
    ('11111111-1111-1111-1111-111111111101'::uuid, 20, 0.0001),
    ('11111111-1111-1111-1111-111111111102'::uuid, 4,  0.0005),
    ('11111111-1111-1111-1111-111111111201'::uuid, 1,  0.0008)
) AS v(place_id, days_ago, lat_offset)
JOIN places p ON p.id = v.place_id;
