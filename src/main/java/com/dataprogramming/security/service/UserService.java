package com.dataprogramming.security.service;

import com.dataprogramming.security.domain.User;
import com.dataprogramming.security.repository.UserRepository;
import com.dataprogramming.security.security.model.RegisterRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public Mono<User> validateUser(String userName, String password) {
        return userRepository.findByUserName(userName)
                .flatMap(user -> {
                    if (passwordEncoder.matches(password, user.getPassword())) {
                        return Mono.just(user);
                    } else {
                        return Mono.empty();
                    }
                })
                .switchIfEmpty(Mono.empty());
    }


    public Mono<User> registerUser(RegisterRequest request) {
        String encryptedPassword = passwordEncoder.encode(request.getPassword());
        User user = User.builder()
                .documentType(request.getDocumentType())
                .documentNumber(request.getDocumentNumber())
                .userName(request.getUserName())
                .password(encryptedPassword)
                .role(request.getRole())
                .enabled(true)
                .build();
        return userRepository.save(user);
    }
}
