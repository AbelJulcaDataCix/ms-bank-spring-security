package com.dataprogramming.security.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.dataprogramming.security.domain.User;
import com.dataprogramming.security.security.model.RegisterRequest;
import com.dataprogramming.security.security.model.RegisterResponse;
import com.dataprogramming.security.util.TestUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;



@ExtendWith(MockitoExtension.class)
class UserMapperTest {

    @Spy
    private UserMapper userMapper = Mappers.getMapper(UserMapper.class);

    @Test
    @DisplayName("maps valid User to RegisterResponse")
    void mapsValidUserToRegisterResponse() {
        User user = TestUtil.readDataFromFileJson("data/user.json", new TypeReference<>() {});

        RegisterResponse response = userMapper.toRegisterResponse(user);

        assertNotNull(response);
        assertEquals(user.getUserName(), response.getUserName());
        assertEquals(user.getDocumentType(), response.getDocumentType());
        assertEquals(user.getDocumentNumber(), response.getDocumentNumber());
        assertEquals(user.getRole(), response.getRole());
    }

    @Test
    @DisplayName("returns Null If User Is Null")
    void returnsNullIfUserIsNull() {
        RegisterResponse response = userMapper.toRegisterResponse(null);
        assertNull(response);
    }

    @Test
    @DisplayName("maps valid RegisterRequest to User ignoring id and enabled")
    void mapsValidRegisterRequestToUserIgnoringIdAndEnabled() {
        RegisterRequest request = TestUtil.readDataFromFileJson
                ("request/registerRequest.json", new TypeReference<>() {});

        User user = userMapper.toUser(request);

        assertNotNull(user);
        assertNull(user.getId());
        assertEquals(request.getUserName(), user.getUserName());
        assertEquals(request.getDocumentType(), user.getDocumentType());
        assertEquals(request.getDocumentNumber(), user.getDocumentNumber());
    }

    @Test
    @DisplayName("returns Null If RegisterRequest Is Null")
    void returnsNullIfRegisterRequestIsNull() {
        User user = userMapper.toUser(null);
        assertNull(user);
    }
}