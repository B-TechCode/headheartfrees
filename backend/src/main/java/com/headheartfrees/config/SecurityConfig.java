package com.headheartfrees.config;

import com.headheartfrees.common.web.SecurityErrorHandler;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * HTTP security for a stateless JSON API.
 *
 * <p>Phase 1 scope: no authentication mechanism exists yet, so this chain only
 * establishes the shape - sessions off, CSRF off (there is no session cookie to
 * protect and no browser form posts), CORS driven by {@link CorsProperties}, and
 * an explicit list of public paths. Login, JWT filtering and role rules arrive in
 * phase 4; the full header and CORS lockdown in phase 9.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** Paths that must stay reachable without an account. */
    private static final String[] PUBLIC_PATHS = {
        "/api/v1/health",
        // Venting never requires an account (PROJECT_BRIEF.md section 2.2).
        "/api/v1/vent/**",
        "/v3/api-docs",
        "/v3/api-docs/**",
        "/swagger-ui.html",
        "/swagger-ui/**",
    };

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http, SecurityErrorHandler securityErrorHandler) throws Exception {
        // withDefaults() picks up the bean named `corsConfigurationSource` below.
        // Injecting CorsConfigurationSource directly is ambiguous: Spring MVC's
        // HandlerMappingIntrospector also implements that interface.
        return http.cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        .anyRequest().authenticated())
                // Rejections here never reach @RestControllerAdvice, so they are
                // rendered in the section 6 error shape by SecurityErrorHandler.
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(securityErrorHandler)
                        .accessDeniedHandler(securityErrorHandler))
                .build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(CorsProperties properties) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(properties.allowedOrigins());
        config.setAllowedMethods(List.of("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        // The refresh token is an httpOnly cookie, so credentials must cross.
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
