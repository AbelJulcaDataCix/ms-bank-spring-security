package com.dataprogramming.security.config.filter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dataprogramming.security.security.jwt.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private WebFilterChain chain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private ServerWebExchange exchange;


    @Test
    @DisplayName("filter With out Authorization Header")
    void filterWithoutAuthorizationHeader() {
        when(chain.filter(any())).thenReturn(Mono.empty());
        exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/test"));

        StepVerifier.create(jwtAuthenticationFilter.filter(exchange, chain))
                .verifyComplete();

        verify(chain).filter(exchange);
    }

    @Test
    @DisplayName("filter With Valid Token")
    void filterWithValidToken() {
        when(chain.filter(any())).thenReturn(Mono.empty());
        String token = "valid.jwt.token";
        exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/test")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token));

        Claims claims = Jwts.claims().setSubject("user1");
        claims.put("role", "ADMIN");

        when(jwtUtil.extractAllClaims(token)).thenReturn(claims);

        Mono<Void> result = jwtAuthenticationFilter.filter(exchange, chain);

        StepVerifier.create(
                result.contextWrite(ctx -> ctx) // Verificar que el contexto tenga Authentication
        ).verifyComplete();

        verify(chain).filter(exchange);
    }

    @Test
    @DisplayName("filter With Invalid Token")
    void filterWithInvalidToken() {
        String token = "invalid.jwt.token";
        exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/test")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token));

        when(jwtUtil.extractAllClaims(token)).thenThrow(new RuntimeException("Invalid token"));

        StepVerifier.create(jwtAuthenticationFilter.filter(exchange, chain))
                .verifyComplete();

        Assertions.assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(exchange);
    }

}