-- FoodMap — lược đồ khởi tạo.
--
-- Tài liệu tương ứng: docs/04-data/erd.md và docs/04-data/data-dictionary.md
-- Quy ước: docs/../.claude/skills/db-migration (ở repo cha)
--
-- ⚠️ File này đã được merge — KHÔNG SỬA. Cần thay đổi thì viết migration mới.

CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS unaccent;   -- tìm kiếm không dấu (FR-PLACE-04)

-- unaccent(text) là STABLE (phụ thuộc search_path), nên Postgres từ chối dùng nó
-- trong biểu thức index. Dạng hai tham số unaccent(regdictionary, text) thì tất định,
-- nên bọc lại thành hàm IMMUTABLE để index dùng được.
CREATE OR REPLACE FUNCTION f_unaccent(text)
RETURNS text
LANGUAGE sql IMMUTABLE PARALLEL SAFE STRICT
AS $func$
    SELECT public.unaccent('public.unaccent', $1)
$func$;

-- ══════════════════════════════ users ══════════════════════════════

CREATE TABLE users (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email             VARCHAR(255) NOT NULL,
    password_hash     VARCHAR(60)  NOT NULL,
    display_name      VARCHAR(100) NOT NULL,
    avatar_url        VARCHAR(500),
    role              VARCHAR(16)  NOT NULL DEFAULT 'USER'
        CONSTRAINT users_role_check CHECK (role IN ('USER', 'MODERATOR', 'ADMIN')),
    preferred_locale  VARCHAR(5)   NOT NULL DEFAULT 'vi'
        CONSTRAINT users_locale_check CHECK (preferred_locale IN ('vi', 'en')),
    email_verified    BOOLEAN      NOT NULL DEFAULT FALSE,
    email_verified_at TIMESTAMPTZ,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at        TIMESTAMPTZ
);

CREATE UNIQUE INDEX uq_users_email ON users (LOWER(email)) WHERE deleted_at IS NULL;
CREATE INDEX idx_users_role ON users (role) WHERE deleted_at IS NULL;

CREATE TABLE refresh_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    -- Lưu HASH của token, không lưu token gốc
    token_hash  VARCHAR(64) NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL,
    revoked_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_refresh_tokens_hash ON refresh_tokens (token_hash);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id);

CREATE TABLE password_reset_tokens (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at    TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_password_reset_hash ON password_reset_tokens (token_hash);

CREATE TABLE email_verification_tokens (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at    TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_email_verification_hash ON email_verification_tokens (token_hash);

-- ═════════════════════════════ places ══════════════════════════════

CREATE TABLE places (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    slug                   VARCHAR(200) NOT NULL,
    place_type             VARCHAR(20)  NOT NULL
        CONSTRAINT places_type_check
        CHECK (place_type IN ('RESTAURANT', 'STREET_FOOD', 'FOOD_MARKET', 'CAFE')),
    -- ⚠️ Dựng bằng ST_MakePoint(KINH ĐỘ, VĨ ĐỘ) — kinh độ trước.
    location               geography(Point, 4326) NOT NULL,
    address                VARCHAR(500),
    phone                  VARCHAR(20),
    status                 VARCHAR(24)  NOT NULL DEFAULT 'DRAFT'
        CONSTRAINT places_status_check
        CHECK (status IN ('DRAFT', 'PUBLISHED', 'TEMPORARILY_CLOSED', 'PERMANENTLY_CLOSED')),
    needs_review           BOOLEAN      NOT NULL DEFAULT FALSE,
    -- NULL khi chưa có đánh giá nào — KHÔNG phải 0 (FR-PLACE-12)
    average_rating         NUMERIC(2, 1)
        CONSTRAINT places_rating_check CHECK (average_rating IS NULL OR average_rating BETWEEN 1.0 AND 5.0),
    review_count           INTEGER      NOT NULL DEFAULT 0,
    visit_count            BIGINT       NOT NULL DEFAULT 0,
    distinct_visitor_count BIGINT       NOT NULL DEFAULT 0,
    created_by             UUID REFERENCES users (id) ON DELETE SET NULL,
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at             TIMESTAMPTZ
);

-- BẮT BUỘC: thiếu index này thì tìm quanh đây quét toàn bảng (ADR-0003)
CREATE INDEX idx_places_location ON places USING GIST (location);
CREATE UNIQUE INDEX uq_places_slug ON places (slug) WHERE deleted_at IS NULL;
CREATE INDEX idx_places_status ON places (status) WHERE deleted_at IS NULL;
CREATE INDEX idx_places_needs_review ON places (needs_review) WHERE needs_review = TRUE;

CREATE TABLE place_translations (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    place_id    UUID         NOT NULL REFERENCES places (id) ON DELETE CASCADE,
    locale      VARCHAR(5)   NOT NULL
        CONSTRAINT place_translations_locale_check CHECK (locale IN ('vi', 'en')),
    name        VARCHAR(255) NOT NULL,
    description TEXT
);

CREATE UNIQUE INDEX uq_place_translations ON place_translations (place_id, locale);
-- Tìm không dấu, không phân biệt hoa thường (FR-PLACE-04).
-- Truy vấn phải dùng ĐÚNG biểu thức này thì mới trúng index:
--   WHERE to_tsvector('simple', f_unaccent(name)) @@ plainto_tsquery('simple', f_unaccent(:q))
CREATE INDEX idx_place_translations_name ON place_translations
    USING GIN (to_tsvector('simple', f_unaccent(name)));

CREATE TABLE categories (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    slug          VARCHAR(80) NOT NULL,
    icon          VARCHAR(80),
    display_order INTEGER     NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_categories_slug ON categories (slug);

CREATE TABLE category_translations (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id UUID         NOT NULL REFERENCES categories (id) ON DELETE CASCADE,
    locale      VARCHAR(5)   NOT NULL
        CONSTRAINT category_translations_locale_check CHECK (locale IN ('vi', 'en')),
    name        VARCHAR(120) NOT NULL
);

CREATE UNIQUE INDEX uq_category_translations ON category_translations (category_id, locale);

CREATE TABLE place_categories (
    place_id    UUID NOT NULL REFERENCES places (id) ON DELETE CASCADE,
    category_id UUID NOT NULL REFERENCES categories (id) ON DELETE CASCADE,
    PRIMARY KEY (place_id, category_id)
);

CREATE INDEX idx_place_categories_category ON place_categories (category_id);

CREATE TABLE opening_hours (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    place_id           UUID     NOT NULL REFERENCES places (id) ON DELETE CASCADE,
    -- 1 = thứ Hai … 7 = Chủ nhật (ISO-8601)
    day_of_week        SMALLINT NOT NULL
        CONSTRAINT opening_hours_dow_check CHECK (day_of_week BETWEEN 1 AND 7),
    open_time          TIME,
    close_time         TIME,
    is_closed_all_day  BOOLEAN  NOT NULL DEFAULT FALSE,
    -- Nghỉ cả ngày thì không có giờ; ngược lại phải có đủ cả hai
    CONSTRAINT opening_hours_time_check CHECK (
        (is_closed_all_day = TRUE  AND open_time IS NULL     AND close_time IS NULL) OR
        (is_closed_all_day = FALSE AND open_time IS NOT NULL AND close_time IS NOT NULL)
    )
);

CREATE INDEX idx_opening_hours_place ON opening_hours (place_id, day_of_week);

CREATE TABLE place_media (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    place_id      UUID         NOT NULL REFERENCES places (id) ON DELETE CASCADE,
    -- Lưu KHOÁ object, không lưu URL đầy đủ — đổi CDN không phải migrate dữ liệu
    storage_key   VARCHAR(500) NOT NULL,
    thumbnail_key VARCHAR(500),
    display_key   VARCHAR(500),
    width         INTEGER,
    height        INTEGER,
    size_bytes    BIGINT       NOT NULL,
    is_cover      BOOLEAN      NOT NULL DEFAULT FALSE,
    display_order INTEGER      NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_place_media_place ON place_media (place_id, display_order);

-- ═════════════════════════════ reviews ═════════════════════════════

CREATE TABLE reviews (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    place_id        UUID        NOT NULL REFERENCES places (id) ON DELETE RESTRICT,
    user_id         UUID        NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    rating          SMALLINT    NOT NULL
        CONSTRAINT reviews_rating_check CHECK (rating BETWEEN 1 AND 5),
    content         TEXT,
    locale          VARCHAR(5)  NOT NULL DEFAULT 'vi'
        CONSTRAINT reviews_locale_check CHECK (locale IN ('vi', 'en')),
    status          VARCHAR(16) NOT NULL DEFAULT 'PENDING'
        CONSTRAINT reviews_status_check
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'HIDDEN')),
    moderation_note TEXT,
    moderated_by    UUID REFERENCES users (id) ON DELETE SET NULL,
    moderated_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at      TIMESTAMPTZ
);

-- Ép luật "một đánh giá mỗi người mỗi địa điểm" ở tầng CSDL (FR-REVIEW-03)
CREATE UNIQUE INDEX uq_reviews_user_place ON reviews (place_id, user_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_reviews_place_status ON reviews (place_id, status) WHERE deleted_at IS NULL;
CREATE INDEX idx_reviews_status_created ON reviews (status, created_at) WHERE deleted_at IS NULL;
CREATE INDEX idx_reviews_user ON reviews (user_id) WHERE deleted_at IS NULL;

CREATE TABLE review_media (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    review_id        UUID         REFERENCES reviews (id) ON DELETE CASCADE,
    uploaded_by      UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    media_type       VARCHAR(10)  NOT NULL
        CONSTRAINT review_media_type_check CHECK (media_type IN ('IMAGE', 'VIDEO')),
    storage_key      VARCHAR(500) NOT NULL,
    thumbnail_key    VARCHAR(500),
    display_key      VARCHAR(500),
    content_type     VARCHAR(50)  NOT NULL,
    width            INTEGER,
    height           INTEGER,
    duration_seconds INTEGER,
    size_bytes       BIGINT       NOT NULL,
    display_order    INTEGER      NOT NULL DEFAULT 0,
    confirmed_at     TIMESTAMPTZ,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_review_media_review ON review_media (review_id, display_order);
-- Media chưa gắn vào đánh giá nào sau 24 giờ sẽ bị dọn (FR-MEDIA-05)
CREATE INDEX idx_review_media_orphan ON review_media (created_at) WHERE review_id IS NULL;

-- ════════════════════════════ feedbacks ════════════════════════════

CREATE TABLE feedbacks (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    place_id              UUID        NOT NULL REFERENCES places (id) ON DELETE CASCADE,
    user_id               UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    type                  VARCHAR(24) NOT NULL
        CONSTRAINT feedbacks_type_check
        CHECK (type IN ('WRONG_ADDRESS', 'WRONG_HOURS', 'CLOSED_PERMANENTLY',
                        'DUPLICATE', 'INAPPROPRIATE', 'OTHER')),
    description           TEXT,
    duplicate_of_place_id UUID REFERENCES places (id) ON DELETE SET NULL,
    status                VARCHAR(16) NOT NULL DEFAULT 'OPEN'
        CONSTRAINT feedbacks_status_check
        CHECK (status IN ('OPEN', 'IN_REVIEW', 'RESOLVED', 'DISMISSED')),
    resolution_note       TEXT,
    resolved_by           UUID REFERENCES users (id) ON DELETE SET NULL,
    resolved_at           TIMESTAMPTZ,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Không cho tạo trùng feedback đang mở (FR-FEEDBACK-03)
CREATE UNIQUE INDEX uq_feedbacks_open ON feedbacks (user_id, place_id, type) WHERE status = 'OPEN';
CREATE INDEX idx_feedbacks_status ON feedbacks (status, created_at);
CREATE INDEX idx_feedbacks_place_type ON feedbacks (place_id, type, created_at);

-- ═══════════════════════ favorites & visits ════════════════════════

CREATE TABLE favorites (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    place_id   UUID        NOT NULL REFERENCES places (id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Khiến thao tác thêm yêu thích idempotent một cách tự nhiên (FR-FAVORITE-02)
CREATE UNIQUE INDEX uq_favorites_user_place ON favorites (user_id, place_id);
CREATE INDEX idx_favorites_user_created ON favorites (user_id, created_at DESC);

CREATE TABLE visits (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id           UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    place_id          UUID        NOT NULL REFERENCES places (id) ON DELETE CASCADE,
    -- Ngày theo giờ Asia/Ho_Chi_Minh, tính lúc ghi bản ghi — KHÔNG phải UTC
    visit_date        DATE        NOT NULL,
    recorded_location geography(Point, 4326) NOT NULL,
    distance_meters   INTEGER     NOT NULL,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Ép luật chống spam 1 lượt/người/địa điểm/ngày (FR-VISIT-02)
CREATE UNIQUE INDEX uq_visits_user_place_day ON visits (user_id, place_id, visit_date);
CREATE INDEX idx_visits_user_created ON visits (user_id, created_at DESC);
CREATE INDEX idx_visits_place ON visits (place_id);

-- ══════════════════════════ notifications ══════════════════════════

CREATE TABLE notifications (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id           UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    type              VARCHAR(32)  NOT NULL
        CONSTRAINT notifications_type_check
        CHECK (type IN ('REVIEW_APPROVED', 'REVIEW_REJECTED', 'FEEDBACK_RESOLVED',
                        'NEW_PLACE_NEARBY', 'PLACE_UPDATED', 'SYSTEM_ANNOUNCEMENT')),
    -- Lưu KEY i18n + tham số, KHÔNG lưu chuỗi đã dịch (người dùng đổi ngôn ngữ được)
    title_key         VARCHAR(100) NOT NULL,
    payload           JSONB        NOT NULL DEFAULT '{}'::jsonb,
    is_read           BOOLEAN      NOT NULL DEFAULT FALSE,
    push_sent_at      TIMESTAMPTZ,
    -- Thông báo bị hoãn qua đêm sẽ có giá trị ở đây (FR-NOTIF-05)
    push_scheduled_at TIMESTAMPTZ,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_notifications_user ON notifications (user_id, created_at DESC);
CREATE INDEX idx_notifications_unread ON notifications (user_id) WHERE is_read = FALSE;
CREATE INDEX idx_notifications_pending_push ON notifications (push_scheduled_at)
    WHERE push_sent_at IS NULL AND push_scheduled_at IS NOT NULL;

CREATE TABLE push_tokens (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token        VARCHAR(255) NOT NULL,
    platform     VARCHAR(10)  NOT NULL
        CONSTRAINT push_tokens_platform_check CHECK (platform IN ('IOS', 'ANDROID')),
    last_used_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_push_tokens_token ON push_tokens (token);
CREATE INDEX idx_push_tokens_user ON push_tokens (user_id);

CREATE TABLE notification_settings (
    user_id       UUID PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    enabled_types JSONB       NOT NULL DEFAULT '[]'::jsonb,
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ═════════════════════════════ chat ════════════════════════════════

CREATE TABLE chat_sessions (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_chat_sessions_user ON chat_sessions (user_id, created_at DESC);

CREATE TABLE chat_messages (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chat_session_id      UUID        NOT NULL REFERENCES chat_sessions (id) ON DELETE CASCADE,
    role                 VARCHAR(10) NOT NULL
        CONSTRAINT chat_messages_role_check CHECK (role IN ('USER', 'ASSISTANT')),
    content              TEXT        NOT NULL,
    -- Id các địa điểm được nhắc tới, để client render thẻ bấm mở (FR-CHAT-05)
    referenced_place_ids JSONB       NOT NULL DEFAULT '[]'::jsonb,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_chat_messages_session ON chat_messages (chat_session_id, created_at);

-- ═══════════════════════════ audit logs ════════════════════════════

-- CHỈ GHI THÊM. Không UPDATE, không DELETE, không xoá mềm (FR-ADMIN-09).
CREATE TABLE audit_logs (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_id     UUID        NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    action       VARCHAR(64) NOT NULL,
    entity_type  VARCHAR(32) NOT NULL,
    entity_id    UUID        NOT NULL,
    before_state JSONB,
    after_state  JSONB,
    note         TEXT,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_logs_entity ON audit_logs (entity_type, entity_id, created_at DESC);
CREATE INDEX idx_audit_logs_actor ON audit_logs (actor_id, created_at DESC);
