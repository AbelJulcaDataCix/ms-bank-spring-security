package com.dataprogramming.security.controller;

import com.dataprogramming.security.domain.User;
import com.dataprogramming.security.mapper.UserMapper;
import com.dataprogramming.security.security.jwt.JwtUtil;
import com.dataprogramming.security.security.model.*;
import com.dataprogramming.security.service.UserService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;

    @PostMapping("/register")
    public Mono<ResponseEntity<RegisterResponse>> register(@RequestBody RegisterRequest request) {

        return userService.userExists(request.getDocumentNumber())
                .flatMap(notExists -> {
                    if (Boolean.TRUE.equals(notExists)) {
                        return userService.registerUser(request)
                                .doOnSuccess(user -> log.info("User registered successfully: {}", user.getUserName()))
                                .doOnError(error -> log.error("Error registering user: {}", error.getMessage()))
                                .map(user -> ResponseEntity.status(HttpStatus.CREATED)
                                        .body(userMapper.toRegisterResponse(user)));
                    } else {
                        log.warn("Attempt to register an existing user: {}", request.getDocumentNumber());
                        return Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).build());
                    }
                });
    }

    @PostMapping("/login")
    public Mono<ResponseEntity<AuthResponse>> login(@RequestBody AuthRequest request) {
        return userService.validateUser(request.getUserName(), request.getPassword())
                .doOnSuccess(user -> log.info("User authenticated successfully"))
                .doOnError(error -> log.error("Authentication failed: {}", error.getMessage()))
                .flatMap(user -> {
                    String token = jwtUtil.generateToken(user);
                    return Mono.just(ResponseEntity.ok(new AuthResponse(token)));
                })
                .switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()));
    }

    @PostMapping("/validate")
    public ResponseEntity<TokenResponse> validateToken(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        String token = extractToken(authHeader);
        try {
            Claims claims = jwtUtil.extractAllClaims(token);
            TokenData tokenData = buildTokenData(token, claims);
            log.info("Token is valid for user: {}", tokenData.getUsername());
            return ResponseEntity.ok(new TokenResponse(true, "Valid token", tokenData));
        } catch (ExpiredJwtException ex) {
            return unauthorizedResponse("The token has expired");
        } catch (JwtException ex) {
            return unauthorizedResponse("Invalid token");
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refreshToken(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        String oldToken = extractToken(authHeader);
        try {

            Claims claims = jwtUtil.extractAllClaims(oldToken);
            User user = buildUserFromClaims(claims);
            String newToken = jwtUtil.generateToken(user);
            TokenData tokenData = buildTokenData(newToken, claims);
            log.info("Token successfully renewed");
            return ResponseEntity.ok(new TokenResponse(true, "Token successfully renewed", tokenData));
        } catch (ExpiredJwtException ex) {
            return unauthorizedResponse("The token has expired, it cannot be refreshed");
        } catch (JwtException ex) {
            return unauthorizedResponse("Invalid token");
        }
    }

    @GetMapping("/users")
    public Flux<UserResponse> getAllUsers() {
        return userService.getAllUsers()
                .map(userMapper::toUserResponse)
                .doOnNext(user -> log.info("Fetched user: {}", user.getUserName()));
    }

    @GetMapping("/users/{id}")
    public Mono<ResponseEntity<UserResponse>> getUserById(@PathVariable String id) {
        return userService.getUserById(id)
                .map(userMapper::toUserResponse)
                .doOnNext(user -> log.info("Fetched user by ID: {}", user.getUserName()))
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/users/{id}")
    public Mono<ResponseEntity<Void>> deleteUserById(@PathVariable String id) {
        return userService.deleteUserById(id)
                .map(deleted -> {
                    if (Boolean.TRUE.equals(deleted)) {
                        log.info("Deleted user with ID: {}", id);
                        return ResponseEntity.noContent().<Void>build();
                    } else {
                        log.warn("Attempt to delete non-existing user with ID: {}", id);
                        return ResponseEntity.notFound().build();
                    }
                });
    }

    private String extractToken(String authHeader) {
        return StringUtils.isNotBlank(authHeader) ? authHeader.replace("Bearer ", "") : "";
    }

    private TokenData buildTokenData(String token, Claims claims) {

        return TokenData.builder()
                .token(token)
                .username(claims.getSubject())
                .role(claims.get("role", String.class))
                .enabled(claims.get("enabled", Boolean.class))
                .build();
    }

    private ResponseEntity<TokenResponse> unauthorizedResponse(String message) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new TokenResponse(false, message, null));
    }

    private User buildUserFromClaims(Claims claims) {

        return User.builder()
                .userName(claims.getSubject())
                .role(claims.get("role", String.class))
                .enabled(claims.get("enabled", Boolean.class))
                .documentType(claims.get("documentType", String.class))
                .documentNumber(claims.get("documentNumber", String.class))
                .build();
    }

}
