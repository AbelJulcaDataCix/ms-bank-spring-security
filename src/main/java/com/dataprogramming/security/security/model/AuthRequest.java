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
public class AuthRequest {
    @NotBlank(message = "The username cannot be empty")
    private String userName;
    @NotBlank(message = "The password cannot be empty")
    private String password;
}
