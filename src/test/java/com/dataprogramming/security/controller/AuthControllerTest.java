package com.dataprogramming.security.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

import com.dataprogramming.security.domain.User;
import com.dataprogramming.security.mapper.UserMapper;
import com.dataprogramming.security.security.jwt.JwtUtil;
import com.dataprogramming.security.security.model.AuthRequest;
import com.dataprogramming.security.security.model.AuthResponse;
import com.dataprogramming.security.security.model.RegisterRequest;
import com.dataprogramming.security.security.model.RegisterResponse;
import com.dataprogramming.security.security.model.TokenData;
import com.dataprogramming.security.security.model.TokenResponse;
import com.dataprogramming.security.service.UserService;
import com.dataprogramming.security.util.TestUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private AuthController authController;

    private User user;
    private RegisterRequest registerRequest;
    private RegisterResponse registerResponse;


    @BeforeEach
    void setUp() {
        user = TestUtil.readDataFromFileJson(
                "data/user.json", new TypeReference<>() {});

        registerRequest = TestUtil.readDataFromFileJson(
                "request/registerRequest.json", new TypeReference<>() {});

        registerResponse = TestUtil.readDataFromFileJson(
                "response/registerResponse.json", new TypeReference<>() {});
    }

    @Test
    @DisplayName("returnsCreatedResponseWhenUserIsRegistered")
    void returnsCreatedResponseWhenUserIsRegistered() {

        when(userService.registerUser(any(RegisterRequest.class))).thenReturn(Mono.just(user));
        when(userMapper.toRegisterResponse(user)).thenReturn(registerResponse);

        Mono<ResponseEntity<RegisterResponse>> result = authController.register(registerRequest);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
                    assertThat(response.getBody()).isNotNull();
                    assertThat(response.getBody().getUserName()).isEqualTo("john_doe");
                    assertThat(response.getBody().getDocumentNumber()).isEqualTo("12345678");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Returns Ok Response With Token When Login Is Successful")
    void returnsOkResponseWithTokenWhenLoginIsSuccessful() {

        AuthRequest authRequest = TestUtil.readDataFromFileJson(
                "request/authRequest.json", new TypeReference<>() {});

        when(userService.validateUser(any(), any())).thenReturn(Mono.just(user));
        when(jwtUtil.generateToken(any())).thenReturn("mocked-jwt-token");

        Mono<ResponseEntity<AuthResponse>> result = authController.login(authRequest);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                    assertThat(response.getBody()).isNotNull();
                    assertThat(response.getBody().getToken()).isEqualTo("mocked-jwt-token");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Returns Unauthorized When User Is Invalid")
    void returnsUnauthorizedWhenUserIsInvalid() {
        AuthRequest authRequest = TestUtil.readDataFromFileJson(
                "request/authRequest.json", new TypeReference<>() {});
        User user = TestUtil.readDataFromFileJson(
                "data/user.json", new TypeReference<>() {});
        // Arrange
        when(userService.validateUser(any(), any())).thenReturn(Mono.empty());
        // Act
        Mono<ResponseEntity<AuthResponse>> result = authController.login(authRequest);
        // Assert
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(response.getBody()).isNull();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("returnsOkResponseWhenTokenIsValid")
    void returnsOkResponseWhenTokenIsValid() {

        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("john_doe");
        when(claims.get("role", String.class)).thenReturn("USER");
        when(claims.get("enabled", Boolean.class)).thenReturn(true);

        when(jwtUtil.extractAllClaims(any())).thenReturn(claims);

        ResponseEntity<TokenResponse> response = authController.validateToken(TestUtil.getToken());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getMessage()).isEqualTo("Valid token");

        TokenData data = response.getBody().getData();
        assertThat(data.getUsername()).isEqualTo("john_doe");
        assertThat(data.getRole()).isEqualTo("USER");
        assertThat(data.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("Returns Unauthorized When Token Is Expired")
    void returnsUnauthorizedWhenTokenIsExpired() {

        when(jwtUtil.extractAllClaims(any())).thenThrow(mock(ExpiredJwtException.class));
        ResponseEntity<TokenResponse> response = authController.validateToken(TestUtil.getToken());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getMessage()).isEqualTo("The token has expired");
    }

    @Test
    @DisplayName("Returns Unauthorized When Token Is Invalid")
    void returnsUnauthorizedWhenTokenIsInvalid() {

        when(jwtUtil.extractAllClaims(any())).thenThrow(mock(JwtException.class));

        ResponseEntity<TokenResponse> response = authController.validateToken(TestUtil.getToken());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid token");
    }

    @Test
    @DisplayName("Returns Ok Response When Token Is Valid And Refreshes Successfully")
    void returnsOkResponseWhenTokenIsValidAndRefreshesSuccessfully() {
        // Arrange
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("john_doe");
        when(claims.get("role", String.class)).thenReturn("USER");
        when(claims.get("enabled", Boolean.class)).thenReturn(true);

        when(jwtUtil.extractAllClaims(any())).thenReturn(claims);
        when(jwtUtil.generateToken(any())).thenReturn(TestUtil.getToken());

        // Act
        ResponseEntity<TokenResponse> response = authController.refreshToken(TestUtil.getToken() );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getMessage()).isEqualTo("Token successfully renewed");

        TokenData data = response.getBody().getData();
        assertThat(data.getToken()).isEqualTo(TestUtil.getToken());
        assertThat(data.getUsername()).isEqualTo("john_doe");
        assertThat(data.getRole()).isEqualTo("USER");
        assertThat(data.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("Returns Unauthorized When Token Is Expired And Cannot Be Refreshed")
    void returnsUnauthorizedWhenTokenIsExpiredAndCannotBeRefreshed() {

        when(jwtUtil.extractAllClaims(any())).thenThrow(mock(ExpiredJwtException.class));

        ResponseEntity<TokenResponse> response = authController.refreshToken(TestUtil.getToken() );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getMessage()).isEqualTo("The token has expired, it cannot be refreshed");
    }

    @Test
    @DisplayName("Returns Unauthorized When Token Is Invalid To Refresh")
    void returnsUnauthorizedWhenTokenIsInvalidToRefresh() {

        when(jwtUtil.extractAllClaims(any())).thenThrow(mock(JwtException.class));

        ResponseEntity<TokenResponse> response = authController.refreshToken(TestUtil.getToken());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid token");
    }

}
