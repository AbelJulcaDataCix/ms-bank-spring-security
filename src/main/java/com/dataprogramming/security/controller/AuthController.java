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
        return userService.registerUser(request)
                .map(user -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(userMapper.toRegisterResponse(user)));
    }


    @PostMapping("/login")
    public Mono<ResponseEntity<AuthResponse>> login(@RequestBody AuthRequest request) {
        return userService.validateUser(request.getUserName(), request.getPassword())
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

            return ResponseEntity.ok(new TokenResponse(true, "Token successfully renewed", tokenData));
        } catch (ExpiredJwtException ex) {
            return unauthorizedResponse("The token has expired, it cannot be refreshed");
        } catch (JwtException ex) {
            return unauthorizedResponse("Invalid token");
        }
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
