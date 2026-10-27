-- Liên kết ngoài của địa điểm (TikTok, Facebook, Instagram, YouTube…) — PlaceDetail.externalLinks (API v1.2.0).
-- Thứ tự hiển thị theo display_order; admin thay toàn bộ danh sách khi cập nhật.

CREATE TABLE place_external_links (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    place_id      UUID          NOT NULL REFERENCES places (id) ON DELETE CASCADE,
    platform      VARCHAR(32)   NOT NULL
        CONSTRAINT place_external_links_platform_check
        CHECK (platform IN ('TIKTOK', 'FACEBOOK', 'INSTAGRAM', 'YOUTUBE', 'OTHER')),
    -- Client mở thẳng URL ra trình duyệt/app ngoài — chỉ chấp nhận https
    url           VARCHAR(2000) NOT NULL
        CONSTRAINT place_external_links_url_https_check
        CHECK (url LIKE 'https://%'),
    title         VARCHAR(200),
    display_order INTEGER       NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_place_external_links_place ON place_external_links (place_id, display_order);
