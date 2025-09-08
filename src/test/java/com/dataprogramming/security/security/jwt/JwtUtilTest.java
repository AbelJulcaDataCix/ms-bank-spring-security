package com.dataprogramming.security.security.jwt;

import com.dataprogramming.security.config.JwtProperties;
import com.dataprogramming.security.domain.User;
import com.dataprogramming.security.util.TestUtil;
import io.jsonwebtoken.Claims;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Date;

@ExtendWith(MockitoExtension.class)
class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        JwtProperties jwtProperties = TestUtil.buildDefaultJwtProperties();
        jwtUtil = new JwtUtil(jwtProperties);
    }


    @Test
    @DisplayName("returnsValidTokenWithClaimsWhenUserIsProvided")
    void returnsValidTokenWithClaimsWhenUserIsProvided() {
        // Arrange
        User user = new User();
        user.setUserName("john_doe");
        user.setRole("ADMIN");
        user.setEnabled(true);
        user.setDocumentType("DNI");
        user.setDocumentNumber("12345678");

        // Act
        String token = jwtUtil.generateToken(user);

        // Assert
        Assertions.assertThat(token).isNotNull();

        Claims claims = jwtUtil.extractAllClaims(token);

        Assertions.assertThat(claims.getSubject()).isEqualTo("john_doe");
        Assertions.assertThat(claims.getIssuer()).isEqualTo("TestIssuer");
        Assertions.assertThat(claims.get("role", String.class)).isEqualTo("ADMIN");
        Assertions.assertThat(claims.get("enabled", Boolean.class)).isTrue();
        Assertions.assertThat(claims.get("documentType", String.class)).isEqualTo("DNI");
        Assertions.assertThat(claims.get("documentNumber", String.class)).isEqualTo("12345678");

        Date expiration = claims.getExpiration();
        Assertions.assertThat(expiration).isAfter(new Date()); // No debe estar vencido
    }
}