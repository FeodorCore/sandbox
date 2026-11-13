package ru.school.helpdesk.config;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import ru.school.helpdesk.security.JwtAuthenticationFilter;
import ru.school.helpdesk.security.RestSecurityErrorHandler;

/**
 * Конфигурация безопасности.
 *
 * <p>Модель доступа:
 * <ul>
 *   <li>{@code ROLE_ADMIN} — всё, включая управление пользователями и категориями;</li>
 *   <li>{@code ROLE_TECHNICIAN} — все заявки, назначение исполнителей, статусы, комментарии;</li>
 *   <li>{@code ROLE_USER} — создание заявок и работа только со своими.</li>
 * </ul>
 *
 * <p>Аутентификация stateless по JWT, поэтому CSRF отключён: сессии и cookie не используются.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /** Публичные пути: вход, статика фронтенда, служебные ресурсы. */
    private static final String[] PUBLIC_MATCHERS = {
            "/api/auth/login",
            "/api/auth/logout",
            "/",
            "/index.html",
            "/favicon.ico",
            "/favicon.svg",
            "/manifest.webmanifest",
            "/robots.txt",
            "/assets/**",
            "/*.js",
            "/*.css",
            "/*.map",
            "/*.svg",
            "/*.png",
            "/*.ico",
            "/*.woff",
            "/*.woff2",
            "/error"
    };

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RestSecurityErrorHandler securityErrorHandler;
    private final AppProperties appProperties;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          RestSecurityErrorHandler securityErrorHandler,
                          AppProperties appProperties) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.securityErrorHandler = securityErrorHandler;
        this.appProperties = appProperties;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(PUBLIC_MATCHERS).permitAll()
                        // Swagger UI доступен только когда springdoc включён (профиль dev) и только администратору
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/api-docs/**")
                        .hasRole("ADMIN")
                        .requestMatchers("/h2-console/**").permitAll()
                        // весь API требует аутентификации, детали прав — в @PreAuthorize контроллеров
                        .requestMatchers("/api/**").authenticated()
                        // остальное — маршруты SPA, их защищает сам фронтенд
                        .anyRequest().permitAll())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(securityErrorHandler)
                        .accessDeniedHandler(securityErrorHandler))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * CORS нужен только в режиме разработки, когда Vite работает на отдельном порту (5173).
     * В production фронтенд раздаёт сам backend, и запросы идут на тот же origin.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        List<String> origins = appProperties.cors() == null || appProperties.cors().allowedOrigins() == null
                ? List.of("http://localhost:5173")
                : appProperties.cors().allowedOrigins();
        configuration.setAllowedOriginPatterns(origins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Content-Disposition"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}


