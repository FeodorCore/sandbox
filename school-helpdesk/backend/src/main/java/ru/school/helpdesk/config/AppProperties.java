package ru.school.helpdesk.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Настройки приложения из {@code application.yml} (префикс {@code app}). */
@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt, Cors cors, Seed seed) {

    /** Параметры JWT. */
    public record Jwt(String secret, Duration expiration, String issuer) {
        public Duration expirationOrDefault() {
            return expiration == null ? Duration.ofHours(12) : expiration;
        }
    }

    /** Разрешённые источники CORS (нужно только для dev-сервера Vite). */
    public record Cors(List<String> allowedOrigins) {
    }

    /** Первоначальное наполнение БД. */
    public record Seed(boolean enabled,
                       String adminUsername,
                       String adminPassword,
                       String adminFullName,
                       boolean demoData) {
    }
}


