# FoodMap Backend — hướng dẫn cho AI

Java 21 · Spring Boot 4.1 · PostgreSQL 16 + PostGIS · Gradle (Kotlin DSL)

Quy ước đầy đủ nằm ở repo cha: skill `spring-backend`, `db-migration`, `foodmap-domain`.
File này chỉ ghi những gì **riêng của repo này** và những chỗ dễ sai.

---

## Chạy

Repo này có **ba module Gradle và hai ứng dụng chạy được**.

```bash
# Hạ tầng phải bật trước (ở repo cha)
../scripts/dev-up.sh

# API công khai — cho app di động. CHẠY CÁI NÀY TRƯỚC: nó dựng lược đồ bằng Flyway.
./gradlew :app-public:bootRun     # http://localhost:8080

# API quản trị — cho trang admin. Cần lược đồ đã tồn tại.
./gradlew :app-admin:bootRun      # http://localhost:8081

./gradlew test                    # cả hai module, cần Docker cho Testcontainers
./gradlew build
```

`./gradlew bootRun` trống **không chạy được** — Gradle không biết chọn app nào.
Luôn ghi rõ `:app-public:` hoặc `:app-admin:`.

Profile mặc định là `local`. Cổng CSDL local là **5433**, Redis **6380** — lệch chuẩn
để không đụng dịch vụ khác trên máy.

---

## Ba module, và luật của chúng

| Module | Là gì | Cổng |
|---|---|---|
| `core` | Thư viện: entity, repository, service, migration Flyway, bảo mật dùng chung | — |
| `app-public` | API cho app di động. **Sở hữu Flyway** | 8080 |
| `app-admin` | API cho trang quản trị | 8081 |

Hai app dùng **chung một database** nhưng là **hai tiến trình riêng**: tách được
connection pool, cấu hình bảo mật, lịch deploy và mức scale.

**Bốn luật không được phá:**

1. **Chỉ `app-public` chạy Flyway.** `app-admin` để `spring.flyway.enabled: false`.
   Bật cả hai thì chúng tranh nhau bảng `flyway_schema_history` và tiến trình khởi động
   sau sẽ chết. Ngoại lệ duy nhất: profile `test` của `app-admin` bật Flyway, vì test
   chạy trên container rỗng của riêng nó.

2. **Lược đồ và entity chỉ định nghĩa ở `core`.** Không tạo entity trong app module.
   Đây là lý do chọn multi-module thay vì hai repo: trình biên dịch bắt được lệch entity
   ngay lúc build, không để tới lúc chạy.

3. **Controller đặt đúng chỗ theo người dùng:**
   - Endpoint cho app di động → `app-public`, package `com.foodmap.<feature>.web`
   - Endpoint `/api/v1/admin/**` → `app-admin`, package `com.foodmap.<feature>.admin`
   - Endpoint **cả hai** cùng phơi (đăng nhập, refresh token) → `core`.
     Cả hai app đều quét `com.foodmap` nên controller ở `core` xuất hiện ở cả hai.

4. **Hai app phải dùng chung `JWT_SECRET`.** Token cấp ở tiến trình này phải đọc được
   ở tiến trình kia. Khác secret thì moderator đăng nhập xong sẽ bị 401 ngay request sau.

Deploy theo thứ tự: **migration trước, `app-public` trước, `app-admin` sau.** Trong lúc
deploy hai tiến trình chạy lệch phiên bản nhau, nên migration phải tương thích ngược —
thêm cột thì được, xoá hay đổi tên cột thì phải tách làm hai lần deploy.

### Cấu hình nằm ở đâu

`core/src/main/resources/foodmap-core.yml` giữ phần dùng chung. Mỗi app nạp nó bằng
`spring.config.import` rồi ghi đè phần riêng. **File của app thắng file core**;
profile riêng (`application-local.yml`, `-dev`, `-prod`) thắng cả hai.

Đừng khai `spring.profiles.active` hay `spring.profiles.default` trong `foodmap-core.yml` —
Spring Boot cấm khai profile trong file được import.

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
core/src/main/java/com.foodmap
├─ config/        WebConfig, OpenApiConfig, JpaConfig, PasswordConfig,
│                 RestAuthenticationHandlers, ApiSecurityDefaults,
│                 props/FoodmapProperties
├─ common/        ApiError, PageResponse, BaseEntity, GeoUtils, exception/
├─ auth/          JwtService, JwtAuthenticationFilter, AuthPrincipal
└─ place/         Place, PlaceRepository, PlaceService, dto/     ← không có controller
core/src/main/resources/
├─ foodmap-core.yml          cấu hình dùng chung
├─ db/migration/ db/dev-data/
└─ messages.properties, messages_en.properties
core/src/testFixtures/       TestcontainersConfiguration (dùng chung cho cả hai app)

app-public/src/main/java/com.foodmap
├─ FoodmapPublicApplication
├─ publicapi/PublicSecurityConfig
└─ place/web/PlaceController

app-admin/src/main/java/com.foodmap
├─ FoodmapAdminApplication
└─ adminapi/AdminSecurityConfig
```

Package-by-feature. Class chỉ dùng trong một feature thì để package-private.
Cần dùng chéo feature thì đi qua service `public`, **không** gọi repository của feature khác.

`ApiSecurityDefaults` giữ phần bảo mật giống nhau giữa hai app (CSRF, CORS, session,
hình dạng lỗi 401/403, vị trí filter JWT). Phần **phải** khác nhau — luật phân quyền theo
đường dẫn — nằm ở `SecurityFilterChain` của từng app. Khác biệt cốt lõi:
`app-public` kết thúc bằng `authenticated()`, `app-admin` kết thúc bằng `denyAll()`.

Feature còn thiếu ở skeleton, sẽ thêm theo lộ trình: `user`, `review`, `feedback`,
`favorite`, `visit`, `media`, `notification`, `chat`.

---

## Dữ liệu

| Thư mục | Chạy ở đâu | Nội dung |
|---|---|---|
| `core/src/main/resources/db/migration/` | mọi môi trường | Lược đồ + dữ liệu tham chiếu (danh mục món ăn) |
| `core/src/main/resources/db/dev-data/` | **chỉ profile `local`** | Địa điểm mẫu để thử API |

Migration nằm ở `core` nhưng **chỉ `app-public` chạy chúng**. `db/dev-data` được nạp nhờ
`spring.flyway.locations` trong `app-public/src/main/resources/application-local.yml`.
Profile `test` cố ý **không** nạp nó — test khẳng định trên lược đồ sạch.

---

## Hợp đồng API

`docs/03-api/openapi.yaml` (submodule `docs` ở repo cha) là **nguồn sự thật**.
Backend implement theo nó, không ngược lại.

Spec sinh từ code ở `/v3/api-docs` chỉ để **đối chiếu** — lệch nhau nghĩa là code đã trôi
khỏi hợp đồng. Chạy subagent `api-contract-guard` để kiểm tra.

---

## Test

- Integration test dùng **Testcontainers với image `postgis/postgis:16-3.4`**.
  Không dùng H2 — H2 không có PostGIS nên test địa lý sẽ vô nghĩa.
- `PostgreSQLContainer` trong Testcontainers 1.21+ là **không generic** — viết
  `new PostgreSQLContainer(image)`, không phải `new PostgreSQLContainer<>(image)`.
- **Đừng khai lại `TestcontainersConfiguration`** trong app module. Nó nằm ở
  `core/src/testFixtures` và dùng chung; khai lại là cách nhanh nhất để hai app trôi
  khỏi nhau (một bên nâng image PostGIS, bên kia thì không). Dùng bằng:
  `testImplementation(testFixtures(project(":core")))` — đã có sẵn ở cả hai app module.
- Spring Boot 4 **dời** `AutoConfigureMockMvc`: package đúng là
  `org.springframework.boot.webmvc.test.autoconfigure`, không phải
  `org.springframework.boot.test.autoconfigure.web.servlet` như Boot 3.
- Mỗi endpoint mới cần tối thiểu: happy path, lỗi validation, và phân quyền (403).
- Endpoint quản trị cần thêm một khẳng định: `app-admin` **không** phục vụ đường dẫn
  của người dùng cuối. Xem `AdminApplicationTests` — mất tính chất đó thì việc tách
  hai tiến trình không còn ý nghĩa.
