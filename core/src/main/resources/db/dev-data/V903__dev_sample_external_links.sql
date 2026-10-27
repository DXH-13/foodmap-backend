-- Dữ liệu MẪU cho môi trường local. KHÔNG chạy ở dev hay production.
--
-- Liên kết ngoài cho vài quán mẫu ở V900. Cố ý dùng URL TÌM KIẾM theo tên quán
-- trên từng nền tảng, không dùng URL bài đăng cụ thể (không bịa nội dung không có thật).
-- Quán 1103, 1104, 1202 không có liên kết — để thử trường hợp externalLinks = [].

INSERT INTO place_external_links (place_id, platform, url, title, display_order) VALUES
    ('11111111-1111-1111-1111-111111111101', 'TIKTOK', 'https://www.tiktok.com/search?q=Ph%E1%BB%9F%20H%C3%B2a%20Pasteur', 'Tìm trên TikTok', 0),
    ('11111111-1111-1111-1111-111111111101', 'FACEBOOK', 'https://www.facebook.com/search/top?q=Ph%E1%BB%9F%20H%C3%B2a%20Pasteur', 'Tìm trên Facebook', 1),
    ('11111111-1111-1111-1111-111111111101', 'INSTAGRAM', 'https://www.instagram.com/explore/search/keyword/?q=Ph%E1%BB%9F%20H%C3%B2a%20Pasteur', 'Tìm trên Instagram', 2),
    ('11111111-1111-1111-1111-111111111101', 'YOUTUBE', 'https://www.youtube.com/results?search_query=Ph%E1%BB%9F%20H%C3%B2a%20Pasteur', 'Tìm trên YouTube', 3),
    ('11111111-1111-1111-1111-111111111102', 'TIKTOK', 'https://www.tiktok.com/search?q=B%C3%A1nh%20m%C3%AC%20Hu%E1%BB%B3nh%20Hoa', 'Tìm trên TikTok', 0),
    ('11111111-1111-1111-1111-111111111102', 'FACEBOOK', 'https://www.facebook.com/search/top?q=B%C3%A1nh%20m%C3%AC%20Hu%E1%BB%B3nh%20Hoa', 'Tìm trên Facebook', 1),
    ('11111111-1111-1111-1111-111111111102', 'INSTAGRAM', 'https://www.instagram.com/explore/search/keyword/?q=B%C3%A1nh%20m%C3%AC%20Hu%E1%BB%B3nh%20Hoa', 'Tìm trên Instagram', 2),
    ('11111111-1111-1111-1111-111111111102', 'YOUTUBE', 'https://www.youtube.com/results?search_query=B%C3%A1nh%20m%C3%AC%20Hu%E1%BB%B3nh%20Hoa', 'Tìm trên YouTube', 3),
    ('11111111-1111-1111-1111-111111111201', 'TIKTOK', 'https://www.tiktok.com/search?q=B%C3%BAn%20ch%E1%BA%A3%20H%C6%B0%C6%A1ng%20Li%C3%AAn', 'Tìm trên TikTok', 0),
    ('11111111-1111-1111-1111-111111111201', 'FACEBOOK', 'https://www.facebook.com/search/top?q=B%C3%BAn%20ch%E1%BA%A3%20H%C6%B0%C6%A1ng%20Li%C3%AAn', 'Tìm trên Facebook', 1),
    ('11111111-1111-1111-1111-111111111201', 'INSTAGRAM', 'https://www.instagram.com/explore/search/keyword/?q=B%C3%BAn%20ch%E1%BA%A3%20H%C6%B0%C6%A1ng%20Li%C3%AAn', 'Tìm trên Instagram', 2),
    ('11111111-1111-1111-1111-111111111201', 'YOUTUBE', 'https://www.youtube.com/results?search_query=B%C3%BAn%20ch%E1%BA%A3%20H%C6%B0%C6%A1ng%20Li%C3%AAn', 'Tìm trên YouTube', 3);
