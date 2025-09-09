package com.dataprogramming.security.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for JWT.
 * <p>
 * This class maps the properties defined in the configuration file
 * (for example, application.properties or application.yml) under the "jwt" prefix.
 * Provides access to the secret key, expiration time, and issuer of the JWT token.
 */

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    private String secret;
    private long expiration;
    private String issuer;
}
