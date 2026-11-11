-- FoodMap — bảng hộp thư đi (outbox) cho sự kiện giữa các module (Bước 4).
--
-- spring-modulith-starter-jpa ghi mỗi sự kiện vào đây trong cùng transaction
-- với thay đổi nghiệp vụ, rồi đánh dấu hoàn thành khi listener xử lý xong.
-- DDL lấy từ phụ lục của Spring Modulith 2.1 (PostgreSQL), đối chiếu với entity
-- org.springframework.modulith.events.jpa.JpaEventPublication trong
-- spring-modulith-events-jpa 2.1.2: 9 cột dưới đây khớp đúng 9 field của entity.
-- Tên bảng viết thường: Postgres tự gấp tên không có dấu ngoặc về chữ thường,
-- nên event_publication khớp với @Table(name = "EVENT_PUBLICATION") của Modulith.
--
-- Hai bảng đều cần: Hibernate đang ở ddl-auto: validate, và starter-jpa đăng ký
-- cả hai entity (chưa hoàn thành + đã lưu trữ). CompletionMode mặc định là UPDATE
-- (đánh dấu hoàn thành tại chỗ) nên bảng lưu trữ thường trống, nhưng thiếu nó thì
-- validate vẫn fail khi khởi động.

CREATE TABLE IF NOT EXISTS event_publication (
    id                     UUID NOT NULL,
    listener_id            TEXT NOT NULL,
    event_type             TEXT NOT NULL,
    serialized_event       TEXT NOT NULL,
    publication_date       TIMESTAMPTZ NOT NULL,
    completion_date        TIMESTAMPTZ,
    status                 TEXT,
    completion_attempts    INT,
    last_resubmission_date TIMESTAMPTZ,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS event_publication_archive (
    id                     UUID NOT NULL,
    listener_id            TEXT NOT NULL,
    event_type             TEXT NOT NULL,
    serialized_event       TEXT NOT NULL,
    publication_date       TIMESTAMPTZ NOT NULL,
    completion_date        TIMESTAMPTZ,
    status                 TEXT,
    completion_attempts    INT,
    last_resubmission_date TIMESTAMPTZ,
    PRIMARY KEY (id)
);
