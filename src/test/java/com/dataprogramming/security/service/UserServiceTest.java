package com.dataprogramming.security.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dataprogramming.security.domain.User;
import com.dataprogramming.security.mapper.UserMapper;
import com.dataprogramming.security.repository.UserRepository;
import com.dataprogramming.security.security.model.RegisterRequest;
import com.dataprogramming.security.util.TestUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Flux;
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

    @Test
    @DisplayName("Returns False When User Exists")
    void returnsFalseWhenUserExists() {

        User user = TestUtil.readDataFromFileJson(
                "data/user.json", new TypeReference<>() {});
        // Arrange
        when(userRepository.findByDocumentNumber(any())).thenReturn(Mono.just(user));

        // Act
        Mono<Boolean> result = userService.userExists("12345678");

        // Assert
        StepVerifier.create(result)
                .expectNext(false)
                .verifyComplete();

        verify(userRepository, times(1)).findByDocumentNumber(any());
    }

    @Test
    @DisplayName("Returns True When User Does Not Exist")
    void returnsTrueWhenUserDoesNotExist() {
        // Arrange
        when(userRepository.findByDocumentNumber(any())).thenReturn(Mono.empty());

        // Act
        Mono<Boolean> result = userService.userExists("87654321");

        // Assert
        StepVerifier.create(result)
                .expectNext(true)
                .verifyComplete();

        verify(userRepository, times(1)).findByDocumentNumber(any());
    }

    @Test
    @DisplayName("Returns All Users Successfully")
    void returnsAllUsersSuccessfully() {
        User user = TestUtil.readDataFromFileJson(
                "data/user.json", new TypeReference<>() {});
        // Arrange
        when(userRepository.findAll()).thenReturn(Flux.just(user));

        // Act
        Flux<User> result = userService.getAllUsers();

        // Assert
        StepVerifier.create(result)
                .expectNext(user)
                .verifyComplete();

        verify(userRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Returns Empty Flux When No Users Exist")
    void returnsEmptyFluxWhenNoUsersExist() {
        // Arrange
        when(userRepository.findAll()).thenReturn(Flux.empty());

        // Act
        Flux<User> result = userService.getAllUsers();

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        verify(userRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Returns User When Exists")
    void returnsUserWhenExists() {

        User user = TestUtil.readDataFromFileJson(
                "data/user.json", new TypeReference<>() {});

        // Arrange
        when(userRepository.findById(anyString())).thenReturn(Mono.just(user));

        // Act
        Mono<User> result = userService.getUserById("1");

        // Assert
        StepVerifier.create(result)
                .expectNextMatches(u -> u.getUserName().equals("john_doe") &&
                        u.getDocumentNumber().equals("12345678"))
                .verifyComplete();

        verify(userRepository, times(1)).findById(anyString());
    }

    @Test
    @DisplayName("Returns Empty When User Find By Id Does Not Exist")
    void returnsEmptyWhenUserFindByIdDoesNotExist() {
        // Arrange
        when(userRepository.findById(anyString())).thenReturn(Mono.empty());

        // Act
        Mono<User> result = userService.getUserById("2");

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        verify(userRepository, times(1)).findById(anyString());
    }

    @Test
    @DisplayName("ReturnsTrueWhenUserIsDeleted")
    void returnsTrueWhenUserIsDeleted() {

        User user = TestUtil.readDataFromFileJson(
                "data/user.json", new TypeReference<>() {});

        // Arrange
        when(userRepository.findById(anyString())).thenReturn(Mono.just(user));
        when(userRepository.delete(any())).thenReturn(Mono.empty());

        // Act
        Mono<Boolean> result = userService.deleteUserById("1");

        // Assert
        StepVerifier.create(result)
                .expectNext(true)
                .verifyComplete();

        verify(userRepository, times(1)).findById(anyString());
        verify(userRepository, times(1)).delete(user);
    }

    @Test
    @DisplayName("ReturnsFalseWhenUserDoesNotExist")
    void returnsFalseWhenUserDoesNotExist() {
        // Arrange
        when(userRepository.findById(anyString())).thenReturn(Mono.empty());

        // Act
        Mono<Boolean> result = userService.deleteUserById("2");

        // Assert
        StepVerifier.create(result)
                .expectNext(false)
                .verifyComplete();

        verify(userRepository, times(1)).findById(anyString());
        verify(userRepository, never()).delete(any());
    }
}