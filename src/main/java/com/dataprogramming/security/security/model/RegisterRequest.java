package com.dataprogramming.security.security.model;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {
    @NotBlank(message = "Document type cannot be blank")
    private String documentType;
    @NotBlank(message = "Document number cannot be blank")
    private String documentNumber;
    @NotBlank(message = "Username cannot be blank")
    private String userName;
    @NotBlank(message = "Password cannot be blank")
    private String password;
    @NotBlank(message = "Role cannot be blank")
    private EnumRole role;

    public enum EnumRole{
        ROLE_USER, ROLE_ADMIN, ROLE_READ, ROLE_WRITE
    }
}
