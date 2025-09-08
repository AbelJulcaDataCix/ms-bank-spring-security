package com.dataprogramming.security.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.dataprogramming.security.domain.User;
import com.dataprogramming.security.mapper.UserMapper;
import com.dataprogramming.security.repository.UserRepository;
import com.dataprogramming.security.security.model.RegisterRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("returns User When User And Password Are Correct")
    void returnsUserWhenUserAndPasswordAreCorrect() {
        User user = new User();
        user.setUserName("abel");
        user.setPassword("12345678");

        when(userRepository.findByUserName(any())).thenReturn(Mono.just(user));
        when(passwordEncoder.matches(any(), any())).thenReturn(true);

        StepVerifier.create(userService.validateUser("abel", "12345678"))
                .expectNext(user)
                .verifyComplete();
    }

    @Test
    @DisplayName("returns Empty When User Does Not Exist")
    void returnsEmptyWhenUserDoesNotExist() {
        when(userRepository.findByUserName(any())).thenReturn(Mono.empty());

        StepVerifier.create(userService.validateUser("noexiste", "cualquier"))
                .verifyComplete();
    }

    @Test
    @DisplayName("returns Empty When Password Is Incorrect")
    void returnsEmptyWhenPasswordIsIncorrect() {
        User user = new User();
        user.setUserName("abel");
        user.setPassword("12345678");

        when(userRepository.findByUserName(any())).thenReturn(Mono.just(user));
        when(passwordEncoder.matches(any(), any())).thenReturn(false);

        StepVerifier.create(userService.validateUser("abel", "wrongpassword"))
                .verifyComplete();
    }

    @Test
    @DisplayName("returns Saved User When RegisterRequest Is Valid")
    void returnsSavedUserWhenRegisterRequestIsValid() {
        RegisterRequest request = new RegisterRequest();
        request.setUserName("abel");
        request.setPassword("plainPassword");

        User user = new User();
        user.setUserName("abel");
        user.setPassword("encryptedPassword");

        when(passwordEncoder.encode("plainPassword")).thenReturn("encryptedPassword");
        when(userMapper.toUser(request)).thenReturn(user);
        when(userRepository.save(user)).thenReturn(Mono.just(user));

        StepVerifier.create(userService.registerUser(request))
                .expectNext(user)
                .verifyComplete();
    }

}