package com.foodmap.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Đặt ở core chứ không ở từng app: hai app phải băm mật khẩu <b>giống hệt nhau</b>,
 * nếu không thì mật khẩu đặt qua app này sẽ không đăng nhập được ở app kia.
 */
@Configuration
public class PasswordConfig {

    /** BCrypt cost 12 (NFR-12). Cao hơn mặc định 10, chậm hơn nhưng khó brute-force hơn. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
