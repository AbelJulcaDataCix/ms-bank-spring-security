package com.dataprogramming.security.mapper;

import com.dataprogramming.security.domain.User;
import com.dataprogramming.security.security.model.RegisterRequest;
import com.dataprogramming.security.security.model.RegisterResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    RegisterResponse toRegisterResponse(User user);

    User toUser(RegisterRequest request);

}
