package com.dataprogramming.security.mapper;

import com.dataprogramming.security.domain.User;
import com.dataprogramming.security.security.model.RegisterRequest;
import com.dataprogramming.security.security.model.RegisterResponse;
import com.dataprogramming.security.security.model.UserResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;


@Mapper(componentModel = "spring")
public interface UserMapper {

    RegisterResponse toRegisterResponse(User user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "enabled", ignore = true)
    User toUser(RegisterRequest request);

    UserResponse toUserResponse(User user);
}
