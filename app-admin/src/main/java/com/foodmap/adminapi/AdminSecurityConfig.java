package com.foodmap.adminapi;

import com.foodmap.auth.JwtAuthenticationFilter;
import com.foodmap.config.ApiSecurityDefaults;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

/**
 * Bảo mật của API quản trị.
 *
 * <p>Khác app-public ở một điểm cốt lõi: kết thúc bằng {@code denyAll()} chứ không phải
 * {@code authenticated()}. Tiến trình này chỉ phục vụ {@code /api/v1/admin/**} và việc
 * đăng nhập — một endpoint dành cho người dùng cuối lọt vào đây cũng không gọi được.
 *
 * <p>Phân quyền chi tiết theo vai trò đặt ở tầng service bằng {@code @PreAuthorize} (NFR-13).
 * Luật đường dẫn dưới đây chỉ là lớp chặn đầu tiên.
 */
@Configuration
@EnableMethodSecurity
public class AdminSecurityConfig {

    /** Chỉ hạ tầng vận hành. Không có endpoint nghiệp vụ nào công khai ở tiến trình này. */
    private static final String[] PUBLIC_GET = {
            "/actuator/health",
            "/actuator/health/**",
            "/actuator/info",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
    };

    /**
     * Moderator đăng nhập qua chính tiến trình này (trang admin gọi
     * {@code POST /api/v1/auth/login} rồi cất token vào cookie httpOnly).
     * Cố ý KHÔNG mở {@code register} và {@code verify-email}: không ai tự đăng ký
     * tài khoản quản trị được.
     */
    private static final String[] PUBLIC_POST = {
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password",
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationFilter jwtFilter,
                                                   AuthenticationEntryPoint entryPoint,
                                                   AccessDeniedHandler accessDeniedHandler) throws Exception {
        return ApiSecurityDefaults.apply(http, jwtFilter, entryPoint, accessDeniedHandler)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.GET, PUBLIC_GET).permitAll()
                        .requestMatchers(HttpMethod.POST, PUBLIC_POST).permitAll()
                        .requestMatchers("/api/v1/admin/**").hasAnyRole("MODERATOR", "ADMIN")
                        // Mọi thứ còn lại không thuộc về tiến trình này (FR-ADMIN-01)
                        .anyRequest().denyAll())
                .build();
    }
}
