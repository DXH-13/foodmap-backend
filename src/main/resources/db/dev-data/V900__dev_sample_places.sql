-- Dữ liệu MẪU cho môi trường local. KHÔNG chạy ở dev hay production.
--
-- Chỉ được nạp khi profile `local` bổ sung classpath:db/dev-data vào
-- spring.flyway.locations (xem application-local.yml).
--
-- Đánh số từ V900 để luôn chạy sau mọi migration thật và không bao giờ va số.
--
-- ⚠️ Nhớ thứ tự: ST_MakePoint(KINH ĐỘ, VĨ ĐỘ) — kinh độ trước.

INSERT INTO places (id, slug, place_type, location, address, status, average_rating, review_count, visit_count)
VALUES
    -- TP.HCM — quanh khu vực Bến Thành
    ('11111111-1111-1111-1111-111111111101', 'pho-hoa-pasteur', 'RESTAURANT',
     ST_SetSRID(ST_MakePoint(106.6889, 10.7891), 4326)::geography,
     '260C Pasteur, Phường 6, Quận 3, TP.HCM', 'PUBLISHED', 4.5, 2, 12),

    ('11111111-1111-1111-1111-111111111102', 'banh-mi-huynh-hoa', 'STREET_FOOD',
     ST_SetSRID(ST_MakePoint(106.6906, 10.7679), 4326)::geography,
     '26 Lê Thị Riêng, Bến Thành, Quận 1, TP.HCM', 'PUBLISHED', 4.2, 1, 30),

    ('11111111-1111-1111-1111-111111111103', 'cho-ben-thanh-khu-an-uong', 'FOOD_MARKET',
     ST_SetSRID(ST_MakePoint(106.6980, 10.7724), 4326)::geography,
     'Chợ Bến Thành, Quận 1, TP.HCM', 'PUBLISHED', NULL, 0, 5),

    ('11111111-1111-1111-1111-111111111104', 'com-tam-ba-ghien', 'RESTAURANT',
     ST_SetSRID(ST_MakePoint(106.6820, 10.7920), 4326)::geography,
     '84 Đặng Văn Ngữ, Phú Nhuận, TP.HCM', 'TEMPORARILY_CLOSED', 4.0, 1, 3),

    ('11111111-1111-1111-1111-111111111105', 'quan-nhap-du-lieu-nhap', 'CAFE',
     ST_SetSRID(ST_MakePoint(106.7000, 10.7800), 4326)::geography,
     'Địa điểm nháp để thử luồng kiểm duyệt', 'DRAFT', NULL, 0, 0),

    -- Hà Nội — quanh Hoàn Kiếm
    ('11111111-1111-1111-1111-111111111201', 'bun-cha-huong-lien', 'RESTAURANT',
     ST_SetSRID(ST_MakePoint(105.8497, 21.0121), 4326)::geography,
     '24 Lê Văn Hưu, Hai Bà Trưng, Hà Nội', 'PUBLISHED', 4.7, 3, 41),

    ('11111111-1111-1111-1111-111111111202', 'pho-bat-dan', 'RESTAURANT',
     ST_SetSRID(ST_MakePoint(105.8480, 21.0327), 4326)::geography,
     '49 Bát Đàn, Hoàn Kiếm, Hà Nội', 'PUBLISHED', 4.4, 2, 18);

-- Tên tiếng Việt — BẮT BUỘC với mọi địa điểm
INSERT INTO place_translations (place_id, locale, name, description) VALUES
    ('11111111-1111-1111-1111-111111111101', 'vi', 'Phở Hoà Pasteur',
     'Quán phở lâu năm ở quận 3, nước dùng đậm, bánh phở mềm.'),
    ('11111111-1111-1111-1111-111111111102', 'vi', 'Bánh mì Huỳnh Hoa',
     'Bánh mì thập cẩm nhiều pate, luôn đông khách buổi chiều.'),
    ('11111111-1111-1111-1111-111111111103', 'vi', 'Khu ăn uống chợ Bến Thành',
     'Nhiều quầy đồ ăn trong chợ, mở cả ngày.'),
    ('11111111-1111-1111-1111-111111111104', 'vi', 'Cơm tấm Ba Ghiền',
     'Sườn nướng miếng to, cơm tấm dẻo.'),
    ('11111111-1111-1111-1111-111111111105', 'vi', 'Quán nháp (dữ liệu thử)',
     'Bản ghi DRAFT để kiểm tra API công khai không trả về nó.'),
    ('11111111-1111-1111-1111-111111111201', 'vi', 'Bún chả Hương Liên',
     'Bún chả Hà Nội, chả nướng than hoa.'),
    ('11111111-1111-1111-1111-111111111202', 'vi', 'Phở Bát Đàn',
     'Phở bò truyền thống, xếp hàng tự lấy.');

-- Tên tiếng Anh — CỐ TÌNH để thiếu ở 3 địa điểm cuối, để kiểm tra fallback về `vi` (FR-I18N-03)
INSERT INTO place_translations (place_id, locale, name, description) VALUES
    ('11111111-1111-1111-1111-111111111101', 'en', 'Pho Hoa Pasteur',
     'Long-standing pho restaurant in District 3, rich broth, soft noodles.'),
    ('11111111-1111-1111-1111-111111111102', 'en', 'Huynh Hoa Banh Mi',
     'Loaded banh mi with plenty of pate; busy every afternoon.'),
    ('11111111-1111-1111-1111-111111111103', 'en', 'Ben Thanh Market Food Court',
     'Many food stalls inside the market, open all day.'),
    ('11111111-1111-1111-1111-111111111201', 'en', 'Huong Lien Bun Cha',
     'Hanoi-style bun cha with charcoal-grilled pork.');

-- Gắn danh mục
INSERT INTO place_categories (place_id, category_id)
SELECT p.id, c.id
FROM (VALUES
    ('11111111-1111-1111-1111-111111111101'::uuid, 'pho'),
    ('11111111-1111-1111-1111-111111111102'::uuid, 'banh-mi'),
    ('11111111-1111-1111-1111-111111111102'::uuid, 'an-vat'),
    ('11111111-1111-1111-1111-111111111103'::uuid, 'an-vat'),
    ('11111111-1111-1111-1111-111111111104'::uuid, 'com'),
    ('11111111-1111-1111-1111-111111111105'::uuid, 'ca-phe'),
    ('11111111-1111-1111-1111-111111111201'::uuid, 'bun'),
    ('11111111-1111-1111-1111-111111111202'::uuid, 'pho')
) AS p(id, category_slug)
JOIN categories c ON c.slug = p.category_slug;

-- Giờ mở cửa: Phở Hoà mở cả ngày; Bánh mì Huỳnh Hoa chỉ bán chiều tối
INSERT INTO opening_hours (place_id, day_of_week, open_time, close_time, is_closed_all_day)
SELECT '11111111-1111-1111-1111-111111111101'::uuid, d, TIME '06:00', TIME '23:30', FALSE
FROM generate_series(1, 7) AS d;

INSERT INTO opening_hours (place_id, day_of_week, open_time, close_time, is_closed_all_day)
SELECT '11111111-1111-1111-1111-111111111102'::uuid, d, TIME '14:30', TIME '23:00', FALSE
FROM generate_series(1, 7) AS d;

-- Phở Bát Đàn: thứ Hai–thứ Bảy bán sáng và tối, nghỉ trưa; Chủ nhật nghỉ cả ngày.
-- Đây là ca kiểm thử cho "nhiều khoảng trong một ngày" (FR-PLACE-07).
INSERT INTO opening_hours (place_id, day_of_week, open_time, close_time, is_closed_all_day)
SELECT '11111111-1111-1111-1111-111111111202'::uuid, d, TIME '06:00', TIME '10:30', FALSE
FROM generate_series(1, 6) AS d;

INSERT INTO opening_hours (place_id, day_of_week, open_time, close_time, is_closed_all_day)
SELECT '11111111-1111-1111-1111-111111111202'::uuid, d, TIME '18:00', TIME '20:30', FALSE
FROM generate_series(1, 6) AS d;

INSERT INTO opening_hours (place_id, day_of_week, open_time, close_time, is_closed_all_day)
VALUES ('11111111-1111-1111-1111-111111111202'::uuid, 7, NULL, NULL, TRUE);
