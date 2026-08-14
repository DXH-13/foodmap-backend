package com.foodmap.config;

import com.foodmap.auth.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Cấu hình bảo mật.
 *
 * <p><b>Mặc định là chặn.</b> Endpoint công khai phải được liệt kê tường minh bên dưới.
 * Thêm endpoint mới mà quên khai ở đây thì nó sẽ yêu cầu đăng nhập — đó là hành vi an toàn.
 *
 * <p>Phân quyền chi tiết theo vai trò đặt ở tầng service bằng {@code @PreAuthorize},
 * không đặt ở controller — để mọi đường vào đều được bảo vệ (NFR-13).
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

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
        return http
                // API stateless dùng JWT — không có session, không có form login, nên CSRF không áp dụng.
                .csrf(csrf -> csrf.disable())
                // Không tiêm CorsConfigurationSource qua tham số: Spring MVC cũng đăng ký
                // một bean cùng kiểu (mvcHandlerMappingIntrospector) nên sẽ nhập nhằng.
                // Dạng mặc định tra cứu đúng bean tên `corsConfigurationSource` ở WebConfig.
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.GET, PUBLIC_GET).permitAll()
                        .requestMatchers(HttpMethod.POST, PUBLIC_POST).permitAll()
                        .requestMatchers("/api/v1/admin/**").hasAnyRole("MODERATOR", "ADMIN")
                        .anyRequest().authenticated())
                // Chưa đăng nhập → 401; đã đăng nhập nhưng thiếu quyền → 403.
                // Mặc định Spring Security trả 403 cho cả hai, khiến client không biết
                // nên mở màn hình đăng nhập hay báo lỗi quyền.
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /** BCrypt cost 12 (NFR-12). Cao hơn mặc định 10, chậm hơn nhưng khó brute-force hơn. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
