package com.foodmap.config;

import com.foodmap.config.props.FoodmapProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.time.Duration;
import java.util.List;
import java.util.Locale;

@Configuration
public class WebConfig {

    /** Hai ngôn ngữ được hỗ trợ. Mặc định tiếng Việt. */
    private static final Locale VIETNAMESE = Locale.forLanguageTag("vi");
    private static final Locale ENGLISH = Locale.ENGLISH;

    /**
     * Ngôn ngữ lấy từ header {@code Accept-Language}, giới hạn trong {vi, en}.
     *
     * <p>Giới hạn là chủ ý: nhờ vậy mọi lần tra cứu chuỗi đều rơi vào
     * {@code messages_vi.properties} hoặc {@code messages_en.properties},
     * không bao giờ cần tới file {@code messages.properties} mặc định.
     */
    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setSupportedLocales(List.of(VIETNAMESE, ENGLISH));
        resolver.setDefaultLocale(VIETNAMESE);
        return resolver;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(FoodmapProperties properties) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(properties.cors().allowedOrigins());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Retry-After"));
        config.setAllowCredentials(true);
        config.setMaxAge(Duration.ofHours(1));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
