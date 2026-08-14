package com.foodmap;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
// Spring Boot 4 dời lớp này khỏi `org.springframework.boot.test.autoconfigure.web.servlet`
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test khói cho tiến trình quản trị.
 *
 * <p>Điều đáng kiểm tra nhất ở đây không phải là "có chạy không", mà là
 * <b>tiến trình này KHÔNG phục vụ API người dùng cuối</b>. Đó là lý do tách app;
 * mất tính chất đó thì việc tách không còn ý nghĩa.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class AdminApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FilterChainProxy securityFilterChain;

    @Test
    @DisplayName("Ngữ cảnh Spring khởi tạo được")
    void contextLoads() {
        assertThat(securityFilterChain).isNotNull();
    }

    @Test
    @DisplayName("Endpoint của người dùng cuối bị chặn ở tiến trình quản trị")
    void publicApiIsNotServedHere() throws Exception {
        // app-public trả 200 cho đường dẫn này mà không cần đăng nhập.
        // Ở đây phải là 401 — luật kết thúc bằng denyAll().
        mockMvc.perform(get("/api/v1/places/nearby")
                        .param("latitude", "10.8231")
                        .param("longitude", "106.6297"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Endpoint quản trị đòi đăng nhập")
    void adminApiRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Health check vẫn mở để nền tảng triển khai thăm dò được")
    void healthEndpointStaysOpen() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }
}
