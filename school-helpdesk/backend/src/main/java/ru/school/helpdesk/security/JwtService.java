package ru.school.helpdesk.security;

import java.util.Date;
import javax.crypto.SecretKey;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Component;
import ru.school.helpdesk.config.AppProperties;
import ru.school.helpdesk.domain.User;

/** Выпуск и проверка JWT-токенов (HS256). */
@Component
public class JwtService {

    private final SecretKey key;
    private final AppProperties.Jwt properties;

    public JwtService(AppProperties appProperties) {
        this.properties = appProperties.jwt();
        byte[] secret = properties.secret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32) {
            throw new IllegalStateException(
                    "app.jwt.secret должен содержать не менее 32 символов (256 бит) для алгоритма HS256");
        }
        this.key = Keys.hmacShaKeyFor(secret);
    }

    public String generateToken(User user) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + properties.expirationOrDefault().toMillis());
        return Jwts.builder()
                .subject(user.getUsername())
                .issuer(properties.issuer())
                .claim("uid", user.getId())
                .claim("role", user.getRole().name())
                .claim("name", user.getFullName())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    public long expiresInSeconds() {
        return properties.expirationOrDefault().toSeconds();
    }

    /** @return имя пользователя из токена или {@code null}, если токен недействителен. */
    public String extractUsername(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(properties.issuer())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.getSubject();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    public boolean isValid(String token) {
        return extractUsername(token) != null;
    }
}


