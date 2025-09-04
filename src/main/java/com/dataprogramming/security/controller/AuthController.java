package com.dataprogramming.security.controller;

import com.dataprogramming.security.domain.User;
import com.dataprogramming.security.security.jwt.JwtUtil;
import com.dataprogramming.security.security.model.*;
import com.dataprogramming.security.service.UserService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
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

    @PostMapping("/register")
    public Mono<ResponseEntity<RegisterResponse>> register(@RequestBody RegisterRequest request) {
        return userService.registerUser(request)
                .map(user -> {
                    // Mapeo de User a RegisterResponse
                    RegisterResponse response = RegisterResponse.builder()
                            .id(user.getId())
                            .documentType(user.getDocumentType())
                            .documentNumber(user.getDocumentNumber())
                            .userName(user.getUserName())
                            .role(user.getRole())
                            .enabled(user.isEnabled())
                            .build();

                    return ResponseEntity.status(HttpStatus.CREATED).body(response);
                });
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
        try {
            String token = authHeader.replace("Bearer ", "");
            Claims claims = jwtUtil.extractAllClaims(token);

            TokenData tokenData = new TokenData(
                    token,
                    claims.getSubject(),
                    claims.get("role", String.class),
                    claims.get("enabled", Boolean.class)
            );

            return ResponseEntity.ok(new TokenResponse(true, "Token válido", tokenData));

        } catch (ExpiredJwtException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new TokenResponse(false, "El token ha expirado", null));
        } catch (JwtException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new TokenResponse(false, "Token inválido", null));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refreshToken(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        try {
            String oldToken = authHeader.replace("Bearer ", "");
            Claims claims = jwtUtil.extractAllClaims(oldToken);

            // Construimos el usuario a partir de los claims
            User user = new User();
            user.setUserName(claims.getSubject());
            user.setRole(claims.get("role", String.class));
            user.setEnabled(claims.get("enabled", Boolean.class));
            user.setDocumentType(claims.get("documentType", String.class));
            user.setDocumentNumber(claims.get("documentNumber", String.class));

            String newToken = jwtUtil.generateToken(user);

            TokenData tokenData = new TokenData(
                    newToken,
                    user.getUserName(),
                    user.getRole(),
                    user.isEnabled()
            );

            return ResponseEntity.ok(new TokenResponse(true, "Token renovado con éxito", tokenData));

        } catch (ExpiredJwtException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new TokenResponse(false, "El token ha expirado, no se puede refrescar", null));
        } catch (JwtException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new TokenResponse(false, "Token inválido", null));
        }
    }


}
