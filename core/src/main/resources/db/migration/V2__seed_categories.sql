-- Danh mục món ăn — dữ liệu tham chiếu, cần ở MỌI môi trường.
--
-- Đây là dữ liệu tham chiếu chứ không phải dữ liệu mẫu: giao diện lọc theo danh mục
-- nên danh sách này phải tồn tại ngay cả trên production.
--
-- Dữ liệu mẫu (địa điểm giả để thử) nằm ở db/dev-data/, chỉ nạp ở profile local.
--
-- ⚠️ File này đã được merge — KHÔNG SỬA. Thêm danh mục mới thì viết migration mới.

INSERT INTO categories (id, slug, icon, display_order) VALUES
    (gen_random_uuid(), 'pho',           'noodle-soup',   10),
    (gen_random_uuid(), 'bun',           'noodle',        20),
    (gen_random_uuid(), 'com',           'rice',          30),
    (gen_random_uuid(), 'banh-mi',       'sandwich',      40),
    (gen_random_uuid(), 'banh-cuon',     'roll',          50),
    (gen_random_uuid(), 'mien-mi',       'noodle',        60),
    (gen_random_uuid(), 'hai-san',       'fish',          70),
    (gen_random_uuid(), 'lau-nuong',     'hotpot',        80),
    (gen_random_uuid(), 'chay',          'leaf',          90),
    (gen_random_uuid(), 'an-vat',        'snack',        100),
    (gen_random_uuid(), 'che-trang-mieng','dessert',      110),
    (gen_random_uuid(), 'ca-phe',        'coffee',       120),
    (gen_random_uuid(), 'tra-sua',       'bubble-tea',   130);

-- Bản dịch tiếng Việt (bắt buộc)
INSERT INTO category_translations (category_id, locale, name)
SELECT c.id, 'vi', v.name
FROM categories c
JOIN (VALUES
    ('pho',            'Phở'),
    ('bun',            'Bún'),
    ('com',            'Cơm'),
    ('banh-mi',        'Bánh mì'),
    ('banh-cuon',      'Bánh cuốn'),
    ('mien-mi',        'Miến & Mì'),
    ('hai-san',        'Hải sản'),
    ('lau-nuong',      'Lẩu & Nướng'),
    ('chay',           'Đồ chay'),
    ('an-vat',         'Ăn vặt'),
    ('che-trang-mieng','Chè & Tráng miệng'),
    ('ca-phe',         'Cà phê'),
    ('tra-sua',        'Trà sữa')
) AS v(slug, name) ON v.slug = c.slug;

-- Bản dịch tiếng Anh (tuỳ chọn, nhưng danh mục thì nên có đủ)
INSERT INTO category_translations (category_id, locale, name)
SELECT c.id, 'en', v.name
FROM categories c
JOIN (VALUES
    ('pho',            'Pho'),
    ('bun',            'Rice noodles'),
    ('com',            'Rice dishes'),
    ('banh-mi',        'Banh mi'),
    ('banh-cuon',      'Steamed rice rolls'),
    ('mien-mi',        'Glass & egg noodles'),
    ('hai-san',        'Seafood'),
    ('lau-nuong',      'Hotpot & BBQ'),
    ('chay',           'Vegetarian'),
    ('an-vat',         'Street snacks'),
    ('che-trang-mieng','Sweet soup & desserts'),
    ('ca-phe',         'Coffee'),
    ('tra-sua',        'Bubble tea')
) AS v(slug, name) ON v.slug = c.slug;
