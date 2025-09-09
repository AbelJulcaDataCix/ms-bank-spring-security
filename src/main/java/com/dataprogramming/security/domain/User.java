package com.dataprogramming.security.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * User entity for the "users" collection in MongoDB.
 * <p>
 * This class represents a system user, including personal information,
 * credentials, and login status.
 * It uses Lombok annotations to automatically generate getters, setters,
 * constructors, and the builder pattern.
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users") // Para MongoDB
public class User {
    @Id
    private String id;
    private String documentType;
    private String documentNumber;
    private String userName;
    private String password;
    private String role;
    private boolean enabled;
}
