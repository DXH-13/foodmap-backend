-- Tìm không dấu trên MÔ TẢ địa điểm (FR-PLACE-04). Tên đã có index từ V1
-- (idx_place_translations_name); GET /api/v1/places tìm cả tên lẫn mô tả.
--
-- Truy vấn phải dùng ĐÚNG biểu thức này thì mới trúng index:
--   WHERE to_tsvector('simple', f_unaccent(description)) @@ plainto_tsquery('simple', f_unaccent(:q))
-- f_unaccent là STRICT nên mô tả NULL cho tsvector NULL — không khớp, đúng ý.

CREATE INDEX idx_place_translations_description ON place_translations
    USING GIN (to_tsvector('simple', f_unaccent(description)));
