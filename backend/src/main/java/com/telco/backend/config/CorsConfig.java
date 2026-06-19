package com.telco.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // Frontend'in çalıştığı tüm portlara ve kökenlere izin ver
        config.setAllowedOriginPatterns(Arrays.asList("*"));

        // Tüm HTTP metodlarına (GET, POST, PUT, DELETE, OPTIONS) izin ver
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // Tüm başlıklara izin ver
        config.setAllowedHeaders(Arrays.asList("*"));

        // Tarayıcının kimlik doğrulama/cookie göndermesine izin ver (Gerekirse)
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config); // Tüm endpointler için geçerli kıl

        return new CorsFilter(source);
    }
}