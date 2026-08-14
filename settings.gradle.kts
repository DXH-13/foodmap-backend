rootProject.name = "foodmap-backend"

// Ba module, hai tiến trình chạy được:
//   core        thư viện dùng chung — entity, repository, service, migration Flyway.
//               KHÔNG bootable. Là nơi DUY NHẤT sở hữu lược đồ CSDL.
//   app-public  API cho ứng dụng di động (cổng 8080). Chạy Flyway lúc khởi động.
//   app-admin   API cho trang quản trị (cổng 8081). KHÔNG chạy Flyway.
//
// Hai app dùng chung một database nhưng là hai tiến trình riêng: tách được
// connection pool, cấu hình bảo mật, lịch deploy và mức scale.
include("core")
include("app-public")
include("app-admin")
