package com.dataprogramming.security.util;

import com.dataprogramming.security.config.JwtProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;


public class TestUtil {

    private static final ObjectMapper mapper;

    static {
        mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
    }

    /**
     * Reads data from a JSON file located on the classpath and deserializes it into the specified type.
     *
     * @param filePath The path to the JSON file within the classpath.
     * @param typeReference Reference to the data type to which you want to deserialize the file's contents.
     * @param <T> The type of object expected as the result.
     * @return an instance of type T with the deserialized data from the JSON file.
     * @throws RuntimeException if an error occurs while reading or deserializing the file.
     */
    public static <T> T readDataFromFileJson(String filePath, TypeReference<T> typeReference) {
        try {
            return mapper.readValue(TestUtil.class.getClassLoader().getResourceAsStream(filePath), typeReference);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns an example JWT token prefixed with "Bearer".
     *
     * @return a string representing an authentication JWT token.
     */
    public static String getToken() {
        return "Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhYmVsIiwianRpIjoiNDcyOGNhZDgt"
                + "MDQ3NS00N2RmLTlkMjAtZTI1MDRjYjMxZDMyIiwiaXNzIjoiZGF0YS1zZW"
                + "N1cml0eSIsImlhdCI6MTc1NzIyMTM0NCwiZXhwIjoxNzU3MjIxNTI0LCJkb"
                + "2N1bWVudFR5cGUiOiIxIiwiZG9jdW1lbnROdW1iZXIiOiI0NzIyMjQxNSIs"
                + "ImVuYWJsZWQiOmZhbHNlLCJyb2xlIjoiUk9MRV9VU0VSIn0.NlMYxLaeoGh"
                + "qYnT8Nnfo-HhDHzHSazidb6M1XjqODjM";
    }

    /**
     * Builds and returns a JwtProperties object with default test values.
     *
     * @return a JwtProperties instance populated with default values for testing.
     */
    public static JwtProperties buildDefaultJwtProperties() {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setSecret("q8cVjsOe3H7kY5q2fF4uT7NzfO5vBGRtK/6L7fjdQfw=");
        jwtProperties.setIssuer("TestIssuer");
        jwtProperties.setExpiration(1000 * 60 * 60);
        return jwtProperties;
    }
}
