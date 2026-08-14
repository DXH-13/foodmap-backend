plugins {
    java
    // Khai version ở đây, `apply false` để module con tự chọn có dùng hay không.
    // Chỉ app-public và app-admin áp plugin Boot (chúng cần bootJar); core thì không.
    id("org.springframework.boot") version "4.1.0" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
}

// Version của thư viện KHÔNG nằm trong BOM của Spring Boot.
// Thư viện Boot đã quản lý thì để trống version, tránh lệch phiên bản.
//
// Phải khai TRƯỚC khối `subprojects` bên dưới: Gradle chạy khối đó ngay khi đọc tới,
// chứ không hoãn tới cuối script.
extra["jjwtVersion"] = "0.13.0"
extra["mapstructVersion"] = "1.6.3"
extra["springdocVersion"] = "3.1.0"
extra["awsSdkVersion"] = "2.53.0"
extra["anthropicVersion"] = "2.54.0"

allprojects {
    group = "vn.foodmap"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "io.spring.dependency-management")

    configure<JavaPluginExtension> {
        toolchain {
            languageVersion = JavaLanguageVersion.of(21)
        }
    }

    // Áp BOM của Spring Boot cho MỌI module, kể cả core (module không áp plugin Boot).
    // Nhờ vậy thư viện Boot ở mọi nơi đều khai không kèm version và không thể lệch nhau.
    configure<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension> {
        imports {
            mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
        }
    }

    dependencies {
        // Lombok + MapStruct: thứ tự annotationProcessor quan trọng
        "compileOnly"("org.projectlombok:lombok")
        "annotationProcessor"("org.projectlombok:lombok")
        "annotationProcessor"("org.projectlombok:lombok-mapstruct-binding:0.2.0")
        "annotationProcessor"("org.mapstruct:mapstruct-processor:${rootProject.extra["mapstructVersion"]}")

        "testCompileOnly"("org.projectlombok:lombok")
        "testAnnotationProcessor"("org.projectlombok:lombok")
        "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    tasks.withType<JavaCompile> {
        // Giữ tên tham số trong bytecode — Spring cần để bind @RequestParam, @PathVariable
        options.compilerArgs.add("-parameters")
    }

    // Tuỳ chọn của MapStruct chỉ áp cho source chính; source test không có processor này.
    tasks.named<JavaCompile>("compileJava") {
        options.compilerArgs.add("-Amapstruct.defaultComponentModel=spring")
    }
}
