package com.dataprogramming.security.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.dataprogramming.security.domain.User;
import com.dataprogramming.security.mapper.UserMapper;
import com.dataprogramming.security.security.jwt.JwtUtil;
import com.dataprogramming.security.security.model.AuthRequest;
import com.dataprogramming.security.security.model.AuthResponse;
import com.dataprogramming.security.security.model.RegisterRequest;
import com.dataprogramming.security.security.model.RegisterResponse;
import com.dataprogramming.security.security.model.TokenData;
import com.dataprogramming.security.security.model.TokenResponse;
import com.dataprogramming.security.security.model.UserResponse;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Objects;

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

        when(userService.userExists(any())).thenReturn(Mono.just(true));
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

    @Test
    @DisplayName("Returns User Responses When Users Exist")
    void returnsUserResponsesWhenUsersExist() {
        // Arrange
        UserResponse userResponse = new UserResponse(
                user.getId(),
                user.getUserName(),
                user.getDocumentNumber(),
                user.isEnabled()
        );

        when(userService.getAllUsers()).thenReturn(Flux.just(user));
        when(userMapper.toUserResponse(any())).thenReturn(userResponse);

        // Act
        Flux<UserResponse> result = authController.getAllUsers();

        // Assert
        StepVerifier.create(result)
                .expectNext(userResponse)
                .verifyComplete();

        verify(userService, times(1)).getAllUsers();
        verify(userMapper, times(1)).toUserResponse(any());
    }

    @Test
    @DisplayName("Returns Empty Flux When No Users Exist")
    void returnsEmptyFluxWhenNoUsersExist() {
        // Arrange
        when(userService.getAllUsers()).thenReturn(Flux.empty());

        // Act
        Flux<UserResponse> result = authController.getAllUsers();

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        verify(userService, times(1)).getAllUsers();
        verifyNoInteractions(userMapper);
    }

    @Test
    @DisplayName("Returns Ok Response When User Exists By Id")
    void returnsOkResponseWhenUserExistsById() {
        // Arrange
        String userId = user.getId();
        UserResponse userResponse = new UserResponse(
                user.getId(),
                user.getUserName(),
                user.getDocumentNumber(),
                user.isEnabled()
        );

        when(userService.getUserById(any())).thenReturn(Mono.just(user));
        when(userMapper.toUserResponse(any())).thenReturn(userResponse);

        // Act
        Mono<ResponseEntity<UserResponse>> result = authController.getUserById(userId);

        // Assert
        StepVerifier.create(result)
                .expectNextMatches(responseEntity ->
                        responseEntity.getStatusCode().is2xxSuccessful() &&
                                Objects.equals(responseEntity.getBody(), userResponse)
                )
                .verifyComplete();

        verify(userService, times(1)).getUserById(anyString());
        verify(userMapper, times(1)).toUserResponse(any());
    }

    @Test
    @DisplayName("Returns Not Found Response When User Does Not Exist")
    void returnsNotFoundResponseWhenUserDoesNotExist() {
        // Arrange
        String userId = "non-existent-id";
        when(userService.getUserById(any())).thenReturn(Mono.empty());

        // Act
        Mono<ResponseEntity<UserResponse>> result = authController.getUserById(userId);

        // Assert
        StepVerifier.create(result)
                .expectNextMatches(responseEntity ->
                        responseEntity.getStatusCode().is4xxClientError() &&
                                responseEntity.getStatusCode().value() == 404
                )
                .verifyComplete();

        verify(userService, times(1)).getUserById(anyString());
        verifyNoInteractions(userMapper);
    }

    @Test
    @DisplayName("Returns No Content When User Is Deleted")
    void returnsNoContentWhenUserIsDeleted() {
        // Arrange
        String userId = "existing-id";
        when(userService.deleteUserById(any())).thenReturn(Mono.just(true));

        // Act
        Mono<ResponseEntity<Void>> result = authController.deleteUserById(userId);

        // Assert
        StepVerifier.create(result)
                .expectNextMatches(response -> response.getStatusCode().is2xxSuccessful() &&
                        response.getStatusCode().value() == 204)
                .verifyComplete();

        verify(userService, times(1)).deleteUserById(anyString());
    }

    @Test
    @DisplayName("Returns Not Found When User Does Not Exist")
    void returnsNotFoundWhenUserDoesNotExist() {
        // Arrange
        String userId = "non-existent-id";
        when(userService.deleteUserById(any())).thenReturn(Mono.just(false));

        // Act
        Mono<ResponseEntity<Void>> result = authController.deleteUserById(userId);

        // Assert
        StepVerifier.create(result)
                .expectNextMatches(response -> response.getStatusCode().is4xxClientError() &&
                        response.getStatusCode().value() == 404)
                .verifyComplete();

        verify(userService, times(1)).deleteUserById(anyString());
    }

}
