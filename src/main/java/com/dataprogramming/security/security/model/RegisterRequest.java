package com.dataprogramming.security.security.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {
    private String documentType;
    private String documentNumber;
    private String userName;
    private String password;
    private String role;
}
