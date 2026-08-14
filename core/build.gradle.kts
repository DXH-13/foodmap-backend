plugins {
    `java-library`
    // Cho phép app-public và app-admin dùng chung hạ tầng test (Testcontainers + PostGIS)
    // mà không phải khai lại. Khai lại là cách nhanh nhất để hai module trôi khỏi nhau.
    `java-test-fixtures`
}

// core KHÔNG áp plugin Spring Boot: nó là thư viện, không phải ứng dụng chạy được.
// Nó chứa entity, repository, service, và — quan trọng nhất — migration Flyway.
// Đây là nơi DUY NHẤT định nghĩa lược đồ CSDL cho cả hai app.

val jjwtVersion = rootProject.extra["jjwtVersion"] as String
val mapstructVersion = rootProject.extra["mapstructVersion"] as String
val springdocVersion = rootProject.extra["springdocVersion"] as String
val awsSdkVersion = rootProject.extra["awsSdkVersion"] as String
val anthropicVersion = rootProject.extra["anthropicVersion"] as String

dependencies {
    // ── `api`: lộ sang app module vì controller ở đó dùng trực tiếp ──
    api("org.springframework.boot:spring-boot-starter-webmvc")
    api("org.springframework.boot:spring-boot-starter-security")
    api("org.springframework.boot:spring-boot-starter-validation")
    api("org.springframework.boot:spring-boot-starter-data-jpa")
    api("org.springframework.boot:spring-boot-starter-actuator")
    // Hibernate Spatial cho kiểu geography(Point,4326)
    api("org.hibernate.orm:hibernate-spatial")
    // Annotation @Operation/@Tag xuất hiện trên controller của cả hai app
    api("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springdocVersion")

    // ── `implementation`: chi tiết nội bộ, app module không cần thấy ──
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-mail")

    // Flyway nằm ở core vì migration nằm ở core. app-admin vẫn có Flyway trên
    // classpath nhưng tắt bằng cấu hình — xem app-admin/src/main/resources/application.yml.
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")

    // ── JWT ──
    implementation("io.jsonwebtoken:jjwt-api:$jjwtVersion")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:$jjwtVersion")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:$jjwtVersion")

    // ── Lưu media: MinIO ở dev, S3 ở prod (cùng API) ──
    implementation(platform("software.amazon.awssdk:bom:$awsSdkVersion"))
    implementation("software.amazon.awssdk:s3")

    // ── Chatbot ──
    implementation("com.anthropic:anthropic-java:$anthropicVersion")

    // ── Mapping DTO ──
    implementation("org.mapstruct:mapstruct:$mapstructVersion")

    // ── Hạ tầng test dùng chung, publish cho app module qua testFixtures ──
    testFixturesApi("org.springframework.boot:spring-boot-testcontainers")
    testFixturesApi("org.testcontainers:testcontainers-junit-jupiter")
    testFixturesApi("org.testcontainers:testcontainers-postgresql")
    testFixturesApi("org.springframework.boot:spring-boot-starter-test")
}
