plugins {
    id("org.springframework.boot")
}

// API quản trị — phục vụ trang admin Next.js. Chạy ở cổng 8081.
// KHÔNG chạy Flyway: lược đồ do app-public quản lý (xem application.yml).

dependencies {
    implementation(project(":core"))

    testImplementation(testFixtures(project(":core")))
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-security-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
}

tasks.bootJar {
    archiveFileName = "foodmap-admin.jar"
}
