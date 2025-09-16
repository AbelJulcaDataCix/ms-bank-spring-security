package com.dataprogramming.security.service;

import com.dataprogramming.security.domain.User;
import com.dataprogramming.security.mapper.UserMapper;
import com.dataprogramming.security.repository.UserRepository;
import com.dataprogramming.security.security.model.RegisterRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    public Mono<User> validateUser(String userName, String password) {
        return userRepository.findByUserName(userName)
                .doOnSuccess(user -> log.info("User found"))
                .doOnError(error -> log.error("Error finding user: {}", error.getMessage()))
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
        request.setPassword(encryptedPassword);
        User user = userMapper.toUser(request);
        user.setEnabled(true);
        return userRepository.save(user);
    }
}
