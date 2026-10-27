-- Tài khoản MẪU cho môi trường local, mỗi vai trò một tài khoản. KHÔNG chạy ở dev hay production.
--
-- Mật khẩu chung cho cả ba: Foodmap@123
-- Hash BCrypt cost 12, sinh bằng pgcrypto: SELECT crypt('Foodmap@123', gen_salt('bf', 12));

INSERT INTO users (id, email, password_hash, display_name, role, preferred_locale, email_verified, email_verified_at)
VALUES
    ('22222222-2222-2222-2222-222222222201', 'admin@foodmap.local',
     '$2a$12$HfCJA7RumERkGjY7SnEhjeMmH7Pu3VuPZ/MqNugXbyReX.WliC6pC',
     'Quản trị viên', 'ADMIN', 'vi', TRUE, now()),

    ('22222222-2222-2222-2222-222222222202', 'moderator@foodmap.local',
     '$2a$12$HfCJA7RumERkGjY7SnEhjeMmH7Pu3VuPZ/MqNugXbyReX.WliC6pC',
     'Kiểm duyệt viên', 'MODERATOR', 'vi', TRUE, now()),

    ('22222222-2222-2222-2222-222222222203', 'user@foodmap.local',
     '$2a$12$HfCJA7RumERkGjY7SnEhjeMmH7Pu3VuPZ/MqNugXbyReX.WliC6pC',
     'Người dùng mẫu', 'USER', 'vi', TRUE, now());
