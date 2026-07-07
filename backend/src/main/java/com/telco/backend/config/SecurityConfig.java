package com.telco.backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod; // 🎯 EKLENDİ: HttpMethod enum'unu kullanabilmek için
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;

    /**
     * 🎯 MADDE 11 / SWAGGER SÜPER BYPASS MOTORU
     * Orijinal Swagger ve OpenAPI rotalarını filtre zincirinin tamamen dışına taşır.
     * Güvenlik katmanı tamamen bypass edildiği için sinsi 403 veya internal forward engelleri kesinlikle yaşanmaz kanka! ✅
     */
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return web -> web.ignoring().requestMatchers(
                "/v3/api-docs",
                "/v3/api-docs/**",
                "/swagger-ui/**",
                "/swagger-ui.html",
                "/swagger-resources/**",
                "/webjars/**"
        );
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 🎯 MADDE 8 GLOBAL CORS: Controller'lardaki kirlilik temizlendi, artık tek merkez burası! ✅
                .cors(cors -> cors.configurationSource(request -> {
                    var corsConfiguration = new org.springframework.web.cors.CorsConfiguration();
                    corsConfiguration.setAllowedOriginPatterns(java.util.List.of(
                            "http://localhost:*",
                            "http://127.0.0.1:*"
                    ));
                    corsConfiguration.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                    corsConfiguration.setAllowedHeaders(java.util.List.of("*"));
                    corsConfiguration.setAllowCredentials(true);
                    return corsConfiguration;
                }))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // 1. Herkese açık çekirdek iş uç noktaları (Swagger mekanizmaları WebSecurityCustomizer seviyesinde bypass edilmiştir kanka)
                        .requestMatchers(
                                "/api/v1/auth/**",
                                "/api/v1/addresses/**",
                                "/api/v1/feasibility/**"
                        ).permitAll()

                        // 🎯 YENİ: ALTYAPI TALEBİ OLUŞTURMA (POST) İŞLEMİ HERKESE AÇIK!
                        // Sadece POST işlemine izin veriyoruz, böylece GET işlemi (listeleme) hala aşağıda güvenli kalıyor.
                        .requestMatchers(HttpMethod.POST, "/api/v1/service-requests").permitAll()

                        // 2. 🎯 MADDE 3 / SİBER GÜVENLİK: Admin dashboard endpoint'lerini sadece ADMIN rolüne kilitliyoruz ✅
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                        // 3. 🎯 MADDE 3 / SİBER GÜVENLİK: OrderController içindeki kapasite artırım operasyonunu sadece ADMIN rolüne kapatıyoruz ✅
                        .requestMatchers("/api/v1/orders/nodes/**").hasRole("ADMIN")

                        // 4. 🎯 MÜŞTERİ PROFİL ZIRHI: Profil bilgilerini getirme ve güncellemeyi sadece sisteme giriş yapmış kullanıcılara açıyoruz ✅
                        .requestMatchers("/api/v1/users/**").authenticated()

                        // 5. Geri kalan tüm talepler (Sipariş verme, geçmiş takibi vs.) sadece giriş yapmış kullanıcılara açık
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}