package com.foodmap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
// Spring Boot 4 dời lớp này khỏi `org.springframework.boot.test.autoconfigure.web.servlet`
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Đăng nhập và làm mới token (FR-AUTH-03, FR-AUTH-04). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class AuthControllerTests {

    private static final String PASSWORD = "matkhau123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private String email;

    @BeforeEach
    void createUser() {
        // Email ngẫu nhiên cho mỗi test — các test dùng chung một CSDL
        email = "User-" + UUID.randomUUID() + "@example.com";
        jdbc.update("""
                INSERT INTO users (email, password_hash, display_name, role)
                VALUES (?, ?, 'Người thử', 'MODERATOR')
                """, email, passwordEncoder.encode(PASSWORD));
    }

    @Test
    @DisplayName("Đăng nhập đúng trả về cặp token, email không phân biệt hoa thường")
    void loginSucceeds() throws Exception {
        login(email.toLowerCase(), PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.expiresIn").value(15 * 60))
                .andExpect(jsonPath("$.user.email").value(email))
                .andExpect(jsonPath("$.user.role").value("MODERATOR"))
                .andExpect(jsonPath("$.user.avatarUrl").value((Object) null))
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("Access token vừa cấp dùng được cho endpoint cần đăng nhập")
    void accessTokenAuthenticates() throws Exception {
        String accessToken = tokens(login(email, PASSWORD)).get("accessToken").asString();

        // Điều cần kiểm ở đây là token được chấp nhận (KHÔNG phải 401); nội dung /me có test riêng
        mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(401));
    }

    @Test
    @DisplayName("Sai mật khẩu và email không tồn tại trả về cùng một lỗi")
    void wrongCredentialsAreIndistinguishable() throws Exception {
        login(email, "saimatkhau1")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));

        login("khongtontai@example.com", PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    @DisplayName("Thiếu trường trả về lỗi validation")
    void loginValidatesInput() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"khong-phai-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("Làm mới token xoay vòng: token cũ không dùng lại được")
    void refreshRotatesToken() throws Exception {
        String first = tokens(login(email, PASSWORD)).get("refreshToken").asString();

        String second = tokens(refresh(first).andExpect(status().isOk())).get("refreshToken").asString();
        assertThat(second).isNotEqualTo(first);

        refresh(first)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
    }

    @Test
    @DisplayName("Dùng lại token đã tiêu thu hồi toàn bộ phiên của người dùng")
    void reuseRevokesAllSessions() throws Exception {
        String stolen = tokens(login(email, PASSWORD)).get("refreshToken").asString();
        String otherDevice = tokens(login(email, PASSWORD)).get("refreshToken").asString();

        refresh(stolen).andExpect(status().isOk());
        refresh(stolen).andExpect(status().isUnauthorized());

        // Phiên trên thiết bị khác cũng phải chết theo
        refresh(otherDevice).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Refresh token không tồn tại bị từ chối")
    void unknownRefreshTokenIsRejected() throws Exception {
        refresh("token-bia-dat")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
    }

    private ResultActions login(String loginEmail, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        java.util.Map.of("email", loginEmail, "password", password))));
    }

    private ResultActions refresh(String refreshToken) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(java.util.Map.of("refreshToken", refreshToken))));
    }

    private JsonNode tokens(ResultActions result) throws Exception {
        return objectMapper.readTree(result.andReturn().getResponse().getContentAsString());
    }
}
