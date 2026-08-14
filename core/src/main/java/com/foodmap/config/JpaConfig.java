package com.foodmap.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/** Bật {@code @CreatedDate} / {@code @LastModifiedDate} trên {@code BaseEntity}. */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
