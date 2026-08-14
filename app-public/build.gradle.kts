plugins {
    id("org.springframework.boot")
}

// API công khai — phục vụ ứng dụng di động. Chạy ở cổng 8080.
// Đây là app SỞ HỮU Flyway: nó chạy migration lúc khởi động (xem application.yml).

dependencies {
    implementation(project(":core"))

    testImplementation(testFixtures(project(":core")))
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-security-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
}

tasks.bootJar {
    archiveFileName = "foodmap-public.jar"
}
