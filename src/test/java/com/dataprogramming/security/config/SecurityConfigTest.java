package com.dataprogramming.security.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.dataprogramming.security.config.filter.JwtAuthenticationFilter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;


@ExtendWith(MockitoExtension.class)
class SecurityConfigTest {

    private final JwtAuthenticationFilter jwtAuthenticationFilter = Mockito.mock(JwtAuthenticationFilter.class);
    private final SecurityConfig securityConfig = new SecurityConfig(jwtAuthenticationFilter);

    @Test
    @DisplayName("PasswordEncoder bean should return BCryptPasswordEncoder")
    void passwordEncoderIsBCrypt() {
        PasswordEncoder encoder = securityConfig.passwordEncoder();
        String raw = "1234";
        String encoded = encoder.encode(raw);
        assertThat(encoder.matches(raw, encoded)).isTrue();
    }

    @Test
    @DisplayName("SecurityWebFilterChain should be created with JwtAuthenticationFilter")
    void securityWebFilterChainIsConfigured() {
        ServerHttpSecurity http = ServerHttpSecurity.http();
        SecurityWebFilterChain chain = securityConfig.securityWebFilterChain(http);

        assertThat(chain).isNotNull();
    }

}