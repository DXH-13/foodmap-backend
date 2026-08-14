package com.foodmap.config;

import com.foodmap.auth.JwtAuthenticationFilter;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Phần cấu hình bảo mật <b>giống nhau</b> giữa app-public và app-admin.
 *
 * <p>Tồn tại để hai app không trôi khỏi nhau ở những chỗ không được phép khác:
 * chính sách session, CSRF, CORS, hình dạng lỗi 401/403 và vị trí của filter JWT.
 * Thứ <b>phải</b> khác nhau — luật phân quyền theo đường dẫn — cố ý không nằm ở đây,
 * mỗi app tự khai trong {@code SecurityFilterChain} của mình.
 */
public final class ApiSecurityDefaults {

    private ApiSecurityDefaults() {
    }

    public static HttpSecurity apply(HttpSecurity http,
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
                // Chưa đăng nhập → 401; đã đăng nhập nhưng thiếu quyền → 403.
                // Mặc định Spring Security trả 403 cho cả hai, khiến client không biết
                // nên mở màn hình đăng nhập hay báo lỗi quyền.
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
    }
}
