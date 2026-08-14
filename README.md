# FoodMap Backend

API của [FoodMap](https://github.com/DXH-13/foodmap) — bản đồ quán ăn, hàng ăn và chợ
đồ ăn Việt Nam.

**Java 21 · Spring Boot 4.1 · PostgreSQL 16 + PostGIS · Redis · Gradle**

Repo này là submodule `backend/` của repo cha.

## Chạy

Hạ tầng (PostGIS, Redis, MinIO, Mailpit) chạy bằng Docker từ repo cha:

```bash
cd ..
./scripts/dev-up.sh          # Windows: .\scripts\dev-up.ps1
cd backend
./gradlew bootRun
```

| Đường dẫn | Nội dung |
|---|---|
| http://localhost:8080/actuator/health | Trạng thái ứng dụng và các phụ thuộc |
| http://localhost:8080/swagger-ui.html | Swagger UI (tắt ở production) |
| http://localhost:8080/v3/api-docs | Spec sinh từ code |

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
src/main/resources/db/migration/    lược đồ + dữ liệu tham chiếu (mọi môi trường)
src/main/resources/db/dev-data/     địa điểm mẫu (CHỈ profile local)
```

**Không sửa migration đã merge.** Cần thay đổi thì viết migration mới.

## Tài liệu

| Nội dung | Ở đâu |
|---|---|
| Hợp đồng API | `docs/03-api/openapi.yaml` (submodule `docs`) |
| Yêu cầu | `docs/01-srs/srs.md` |
| Lược đồ dữ liệu | `docs/04-data/erd.md` |
| Truy vấn địa lý | `docs/04-data/geo-model.md` |
| Quy ước code | [`CLAUDE.md`](./CLAUDE.md) và skill `spring-backend` ở repo cha |
