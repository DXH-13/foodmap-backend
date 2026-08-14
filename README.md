# FoodMap Backend

API của [FoodMap](https://github.com/DXH-13/foodmap) — bản đồ quán ăn, hàng ăn và chợ
đồ ăn Việt Nam.

**Java 21 · Spring Boot 4.1 · PostgreSQL 16 + PostGIS · Redis · Gradle**

Repo này là submodule `backend/` của repo cha.

## Cấu trúc

Ba module Gradle, **hai ứng dụng chạy được**, dùng chung một database:

| Module | Vai trò | Cổng |
|---|---|---|
| `core` | Thư viện: entity, repository, service, migration Flyway. Không tự chạy được | — |
| `app-public` | API cho ứng dụng di động. **Chạy Flyway** lúc khởi động | 8080 |
| `app-admin` | API cho trang quản trị. Không chạy Flyway | 8081 |

Tách tiến trình để một truy vấn thống kê nặng ở trang quản trị không hút hết connection
pool rồi làm chậm app di động, và để hai bên deploy độc lập. Lược đồ vẫn chỉ có một bản,
ở `core` — đó là lý do chọn multi-module thay vì hai repo riêng.

## Chạy

Hạ tầng (PostGIS, Redis, MinIO, Mailpit) chạy bằng Docker từ repo cha:

```bash
cd ..
./scripts/dev-up.sh          # Windows: .\scripts\dev-up.ps1
cd backend

# Chạy cái này TRƯỚC — nó dựng lược đồ bằng Flyway
./gradlew :app-public:bootRun

# Cửa sổ khác
./gradlew :app-admin:bootRun
```

`./gradlew bootRun` trống sẽ báo lỗi — phải ghi rõ module.

| Đường dẫn | Nội dung |
|---|---|
| http://localhost:8080/actuator/health | Trạng thái API công khai |
| http://localhost:8080/swagger-ui.html | Swagger UI của API công khai (tắt ở production) |
| http://localhost:8080/v3/api-docs | Spec sinh từ code, API công khai |
| http://localhost:8081/actuator/health | Trạng thái API quản trị |
| http://localhost:8081/swagger-ui.html | Swagger UI của API quản trị |

Thử nhanh endpoint tìm quanh đây (dữ liệu mẫu có sẵn ở profile `local`):

```bash
curl "http://localhost:8080/api/v1/places/nearby?latitude=10.7724&longitude=106.6980&radiusMeters=3000"
```

## Test

```bash
./gradlew test
```

Cần Docker — integration test dùng Testcontainers với image PostGIS thật.

## Cấu hình

Toàn bộ qua biến môi trường. Mẫu đầy đủ: `infra/.env.example` ở repo cha.

Bắt buộc điền trước khi chạy production: `JWT_SECRET`, `GOOGLE_MAPS_API_KEY`,
`ANTHROPIC_API_KEY`, và các biến `S3_*`.

Profile: `local` (mặc định) · `dev` · `prod` · `test` (chỉ dùng khi chạy test).

## Cơ sở dữ liệu

Lược đồ do **Flyway** quản lý. `spring.jpa.hibernate.ddl-auto` luôn là `validate`.

```
core/src/main/resources/db/migration/    lược đồ + dữ liệu tham chiếu (mọi môi trường)
core/src/main/resources/db/dev-data/     địa điểm mẫu (CHỈ profile local)
```

Migration nằm ở `core` nhưng **chỉ `app-public` chạy chúng**. `app-admin` để
`spring.flyway.enabled: false` — bật cả hai thì hai tiến trình sẽ tranh nhau bảng
`flyway_schema_history`. Deploy theo thứ tự: `app-public` trước, `app-admin` sau.

**Không sửa migration đã merge.** Cần thay đổi thì viết migration mới.

## Tài liệu

| Nội dung | Ở đâu |
|---|---|
| Hợp đồng API | `docs/03-api/openapi.yaml` (submodule `docs`) |
| Yêu cầu | `docs/01-srs/srs.md` |
| Lược đồ dữ liệu | `docs/04-data/erd.md` |
| Truy vấn địa lý | `docs/04-data/geo-model.md` |
| Quy ước code | [`CLAUDE.md`](./CLAUDE.md) và skill `spring-backend` ở repo cha |
