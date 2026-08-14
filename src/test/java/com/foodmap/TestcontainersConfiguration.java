package com.foodmap;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Hạ tầng cho integration test.
 *
 * <p><b>Bắt buộc dùng image PostGIS, không dùng H2 và không dùng postgres thuần.</b>
 * Tính năng cốt lõi của FoodMap là truy vấn địa lý; test trên CSDL không có PostGIS
 * thì hoặc là không chạy được, hoặc là chạy được nhưng không chứng minh điều gì.
 * Xem ADR-0003 và NFR-20.
 */
@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

    private static final DockerImageName POSTGIS_IMAGE = DockerImageName
            .parse("postgis/postgis:16-3.4")
            .asCompatibleSubstituteFor("postgres");

    @Bean
    @ServiceConnection
    @SuppressWarnings("resource") // Vòng đời container do Spring quản lý
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(POSTGIS_IMAGE)
                .withDatabaseName("foodmap")
                .withUsername("foodmap")
                .withPassword("foodmap");
    }

    @Bean
    @ServiceConnection(name = "redis")
    @SuppressWarnings("resource")
    GenericContainer<?> redisContainer() {
        return new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                .withExposedPorts(6379);
    }
}
