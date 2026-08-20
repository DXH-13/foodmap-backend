plugins {
    java
    id("org.springframework.boot") version "4.1.0"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "vn.foodmap"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

// Chỉ ghi version cho thư viện KHÔNG nằm trong BOM của Spring Boot.
// Thư viện Boot đã quản lý thì để trống version, tránh lệch phiên bản.
val jjwtVersion = "0.13.0"
val mapstructVersion = "1.6.3"
val springdocVersion = "3.1.0"
val awsSdkVersion = "2.53.0"
val anthropicVersion = "2.54.0"

dependencies {
    // ── Spring Boot ──
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // ── CSDL: PostgreSQL + PostGIS + Flyway ──
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.flywaydb:flyway-database-postgresql")
    // Hibernate Spatial cho kiểu geography(Point,4326)
    implementation("org.hibernate.orm:hibernate-spatial")
    runtimeOnly("org.postgresql:postgresql")

    // ── JWT ──
    implementation("io.jsonwebtoken:jjwt-api:$jjwtVersion")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:$jjwtVersion")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:$jjwtVersion")

    // ── Sinh spec từ code, để đối chiếu với docs/SDD/api/openapi.yaml ──
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springdocVersion")

    // ── Lưu media: MinIO ở dev, S3 ở prod (cùng API) ──
    implementation(platform("software.amazon.awssdk:bom:$awsSdkVersion"))
    implementation("software.amazon.awssdk:s3")

    // ── Chatbot ──
    implementation("com.anthropic:anthropic-java:$anthropicVersion")

    // ── Mapping DTO ──
    implementation("org.mapstruct:mapstruct:$mapstructVersion")

    // ── Lombok + MapStruct: thứ tự annotationProcessor quan trọng ──
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok-mapstruct-binding:0.2.0")
    annotationProcessor("org.mapstruct:mapstruct-processor:$mapstructVersion")

    // ── Test ──
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-security-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-postgresql")
    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<JavaCompile> {
    // Giữ tên tham số trong bytecode — Spring cần để bind @RequestParam, @PathVariable
    options.compilerArgs.add("-parameters")
}

// Tuỳ chọn của MapStruct chỉ áp cho source chính; source test không có processor này.
tasks.compileJava {
    options.compilerArgs.add("-Amapstruct.defaultComponentModel=spring")
}
