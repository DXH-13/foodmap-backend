package com.foodmap.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cấu hình spec sinh từ code, phơi ở {@code /v3/api-docs}.
 *
 * <p>Spec này <b>không</b> phải hợp đồng — hợp đồng là
 * {@code docs/SDD/api/openapi.yaml}. Nó tồn tại để <b>đối chiếu</b>: lệch nhau nghĩa là
 * code đã trôi khỏi hợp đồng. Xem subagent {@code api-contract-guard}.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI foodmapOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("FoodMap API (sinh từ code)")
                        .version("1.0.0")
                        .description("""
                                Spec này được sinh từ mã nguồn để đối chiếu với hợp đồng \
                                docs/SDD/api/openapi.yaml. Hợp đồng mới là nguồn sự thật."""))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}
