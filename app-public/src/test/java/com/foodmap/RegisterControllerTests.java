package com.foodmap;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** POST /api/v1/auth/register (FR-AUTH-01). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class RegisterControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static String newEmail() {
        return "Moi-" + UUID.randomUUID() + "@example.com";
    }

    @Test
    @DisplayName("Đăng ký không cần token: 201, có phiên ngay, chưa xác minh email, nhận preferredLocale")
    void registerSucceeds() throws Exception {
        String email = newEmail();
        String body = register(Map.of("email", email, "password", "matkhau123",
                "displayName", "  Người mới  ", "preferredLocale", "en"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.expiresIn").value(15 * 60))
                .andExpect(jsonPath("$.user.email").value(email))
                .andExpect(jsonPath("$.user.displayName").value("Người mới"))
                .andExpect(jsonPath("$.user.role").value("USER"))
                .andExpect(jsonPath("$.user.preferredLocale").value("en"))
                .andExpect(jsonPath("$.user.emailVerified").value(false))
                .andExpect(jsonPath("$.user.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        String accessToken = objectMapper.readTree(body).get("accessToken").asString();
        mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));

        // Đăng nhập lại bằng mật khẩu vừa đặt
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", "matkhau123"))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Không gửi preferredLocale thì mặc định vi")
    void defaultLocaleIsVi() throws Exception {
        register(Map.of("email", newEmail(), "password", "matkhau123", "displayName", "A"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.preferredLocale").value("vi"));
    }

    @Test
    @DisplayName("Email đã đăng ký (khác hoa thường) trả 409 EMAIL_ALREADY_EXISTS")
    void duplicateEmailConflicts() throws Exception {
        String email = newEmail();
        register(Map.of("email", email, "password", "matkhau123", "displayName", "A"))
                .andExpect(status().isCreated());

        register(Map.of("email", email.toUpperCase(), "password", "matkhau456", "displayName", "B"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.message").value("Email này đã được đăng ký."))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    @DisplayName("Mật khẩu yếu (ngắn, thiếu số, thiếu chữ) trả 400 WEAK_PASSWORD, thông báo đã dịch")
    void weakPasswordIsRejected() throws Exception {
        for (String weak : new String[]{"abc123", "matkhaudai", "12345678"}) {
            register(Map.of("email", newEmail(), "password", weak, "displayName", "A"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("WEAK_PASSWORD"));
        }
        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .header("Accept-Language", "en")
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", newEmail(), "password", "short1", "displayName", "A"))))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("8")));
    }

    @Test
    @DisplayName("Thiếu trường, email sai định dạng, locale lạ trả 400 VALIDATION_FAILED")
    void invalidInputIsRejected() throws Exception {
        register(Map.of("email", "khong-phai-email", "password", "matkhau123", "displayName", "A"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details[0].field").value("email"));

        register(Map.of("email", newEmail(), "password", "matkhau123"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        Map<String, Object> badLocale = new HashMap<>(
                Map.of("email", newEmail(), "password", "matkhau123", "displayName", "A"));
        badLocale.put("preferredLocale", "fr");
        register(badLocale)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    private ResultActions register(Map<String, ?> body) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }
}
