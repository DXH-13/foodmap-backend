# FoodMap Backend — hướng dẫn cho AI

Java 21 · Spring Boot 4.1 · PostgreSQL 16 + PostGIS · Gradle (Kotlin DSL)

Quy ước đầy đủ nằm ở repo cha: skill `spring-backend`, `db-migration`, `foodmap-domain`.
File này chỉ ghi những gì **riêng của repo này** và những chỗ dễ sai.

---

## Chạy

```bash
# Hạ tầng phải bật trước (ở repo cha)
../scripts/dev-up.sh

./gradlew bootRun      # http://localhost:8080
./gradlew test         # cần Docker cho Testcontainers
./gradlew build
```

Profile mặc định là `local` (khai trong `application.yml`). Cổng CSDL local là **5433**,
Redis **6380** — lệch chuẩn để không đụng dịch vụ khác trên máy.

---

## Bốn thứ dễ sai nhất trong repo này

### 1. Spring Boot 4 dùng Jackson 3

Package là **`tools.jackson`**, không phải `com.fasterxml.jackson`.
Import sai thì bean `ObjectMapper` sẽ không inject được và ứng dụng không khởi động.

```java
import tools.jackson.databind.ObjectMapper;   // ĐÚNG
```

Nhiều thuộc tính `spring.jackson.*` quen thuộc cũng đã đổi hoặc bị bỏ. Cấu hình mặc định
của Boot đã đúng nhu cầu (ISO-8601, giữ field null) — đừng thêm gì nếu chưa có lý do rõ ràng.

### 2. Bản tiếng Việt nằm ở `messages.properties`, không phải `messages_vi.properties`

Spring Boot chỉ tạo `MessageSource` khi tìm thấy đúng `<basename>.properties`. Thiếu nó,
mọi thông báo lỗi sẽ trả về chính cái key. Lỗi này âm thầm — chỉ lộ ra khi thấy
`place.error.not_found` hiện lên màn hình.

Ngoài ra: trong `AuthenticationEntryPoint` / `AccessDeniedHandler`, `LocaleContextHolder`
**chưa** được đặt (filter chạy trước DispatcherServlet) — phải tự đọc header
`Accept-Language`. Xem `RestAuthenticationHandlers`.

### 3. Toạ độ: `ST_MakePoint(kinh_độ, vĩ_độ)`

Kinh độ trước. Nhầm thứ tự thì điểm rơi ra giữa Ấn Độ Dương và không có gì báo lỗi.

**Chỉ dùng `GeoUtils.toPoint(lat, lng)`.** Đừng gọi `new Coordinate(...)` ở nơi khác.

Và trong truy vấn: lọc bằng `ST_DWithin` (dùng được index GiST), **không** dùng
`ST_Distance(...) < x` trong `WHERE` (quét toàn bảng — kết quả vẫn đúng nên rất khó phát hiện).

Native query dùng `CAST(... AS geography)`, **không** dùng cú pháp `::geography` — dấu
hai chấm đôi xung đột với cách Hibernate phân tích tham số `:name`.

### 4. Migration đã merge thì không sửa

Flyway lưu checksum; sửa file cũ làm mọi môi trường đã chạy nó hỏng ngay lần khởi động sau.
Sai thì viết migration mới. Ngoại lệ duy nhất: migration chưa push khỏi máy bạn.

`ddl-auto` luôn là `validate` ở mọi profile. Lược đồ do Flyway quản lý.

---

## Cấu trúc

```
com.foodmap
├─ config/        SecurityConfig, WebConfig, OpenApiConfig, JpaConfig,
│                 RestAuthenticationHandlers, props/FoodmapProperties
├─ common/        ApiError, PageResponse, BaseEntity, GeoUtils, exception/
├─ auth/          JwtService, JwtAuthenticationFilter, AuthPrincipal
└─ place/         Place, PlaceRepository, PlaceService, PlaceController, dto/
```

Package-by-feature. Class chỉ dùng trong một feature thì để package-private.
Cần dùng chéo feature thì đi qua service `public`, **không** gọi repository của feature khác.

Module còn thiếu ở skeleton, sẽ thêm theo lộ trình: `user`, `review`, `feedback`,
`favorite`, `visit`, `media`, `notification`, `chat`, `admin`.

---

## Dữ liệu

| Thư mục | Chạy ở đâu | Nội dung |
|---|---|---|
| `db/migration/` | mọi môi trường | Lược đồ + dữ liệu tham chiếu (danh mục món ăn) |
| `db/dev-data/` | **chỉ profile `local`** | Địa điểm mẫu để thử API |

`db/dev-data` được nạp nhờ `spring.flyway.locations` trong `application-local.yml`.
Profile `test` cố ý **không** nạp nó — test khẳng định trên lược đồ sạch.

---

## Hợp đồng API

`docs/SDD/api/openapi.yaml` (submodule `docs` ở repo cha) là **nguồn sự thật**.
Backend implement theo nó, không ngược lại.

Spec sinh từ code ở `/v3/api-docs` chỉ để **đối chiếu** — lệch nhau nghĩa là code đã trôi
khỏi hợp đồng. Chạy subagent `api-contract-guard` để kiểm tra.

---

## Test

- Integration test dùng **Testcontainers với image `postgis/postgis:16-3.4`**.
  Không dùng H2 — H2 không có PostGIS nên test địa lý sẽ vô nghĩa.
- `PostgreSQLContainer` trong Testcontainers 1.21+ là **không generic** — viết
  `new PostgreSQLContainer(image)`, không phải `new PostgreSQLContainer<>(image)`.
- Mỗi endpoint mới cần tối thiểu: happy path, lỗi validation, và phân quyền (403).
