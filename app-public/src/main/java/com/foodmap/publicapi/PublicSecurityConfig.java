package com.foodmap.publicapi;

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
 * Bảo mật của API công khai.
 *
 * <p><b>Mặc định là chặn.</b> Endpoint công khai phải được liệt kê tường minh bên dưới.
 * Thêm endpoint mới mà quên khai ở đây thì nó sẽ yêu cầu đăng nhập — đó là hành vi an toàn.
 *
 * <p>Phân quyền chi tiết theo vai trò đặt ở tầng service bằng {@code @PreAuthorize},
 * không đặt ở controller — để mọi đường vào đều được bảo vệ (NFR-13).
 */
@Configuration
@EnableMethodSecurity
public class PublicSecurityConfig {

    /** Danh sách endpoint khách vãng lai truy cập được (FR-PLACE-01). */
    private static final String[] PUBLIC_GET = {
            "/api/v1/places/**",
            "/api/v1/categories",
            "/actuator/health",
            "/actuator/health/**",
            "/actuator/info",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
    };

    private static final String[] PUBLIC_POST = {
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password",
            "/api/v1/auth/verify-email",
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationFilter jwtFilter,
                                                   AuthenticationEntryPoint entryPoint,
                                                   AccessDeniedHandler accessDeniedHandler) throws Exception {
        return ApiSecurityDefaults.apply(http, jwtFilter, entryPoint, accessDeniedHandler)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Tiến trình này KHÔNG phục vụ API quản trị. Chặn thẳng thay vì
                        // dựa vào việc "ở đây không có controller nào" — một controller
                        // quản trị bị đặt nhầm module sẽ không lọt ra internet.
                        .requestMatchers("/api/v1/admin/**").denyAll()
                        .requestMatchers(HttpMethod.GET, PUBLIC_GET).permitAll()
                        .requestMatchers(HttpMethod.POST, PUBLIC_POST).permitAll()
                        .anyRequest().authenticated())
                .build();
    }
}
