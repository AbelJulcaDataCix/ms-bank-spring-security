package com.dataprogramming.security.security.jwt;

import com.dataprogramming.security.config.JwtProperties;
import com.dataprogramming.security.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class JwtUtil {

    private final JwtProperties jwtProperties;
    private final Key key;

    public JwtUtil(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.key = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes());
    }

    /**
     * Generates a JWT token for the provided user.
     *
     * @param user : The user for whom the token will be generated.
     * @return the generated JWT token as a string.
     * The token includes the following custom claims:
     * - role: The user's role.
     * - enabled: Whether the user is enabled.
     * - documentType: The user's document type.
     * - documentNumber: The user's document number.
     * The token also includes standard information such as the subject (username),
     * the issuer, the issue date, the expiration date, and a unique identifier.
     */
    public String generateToken(User user) {
        log.info("Generating token for user: {}", user.getUserName());
        return Jwts.builder()
                .setSubject(user.getUserName())
                .setId(UUID.randomUUID().toString())
                .setIssuer(jwtProperties.getIssuer())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtProperties.getExpiration()))
                .addClaims(Map.of(
                        "role", user.getRole(),
                        "enabled", user.isEnabled(),
                        "documentType", user.getDocumentType(),
                        "documentNumber", user.getDocumentNumber()
                ))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

}
