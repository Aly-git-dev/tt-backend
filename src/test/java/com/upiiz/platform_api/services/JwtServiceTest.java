package com.upiiz.platform_api.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JwtServiceTest {

    private static final String TEST_SECRET = "test-secret-key-with-at-least-32-characters!!";

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void accessTokenExpirationIsCappedAtThirtyDays() throws Exception {
        JwtService jwt = new JwtService(
                TEST_SECRET,
                "test",
                999_999,
                30
        );
        jwt.init();

        Map<String, Object> payload = payload(jwt.access(Map.of(), "user@alumno.ipn.mx"));

        assertEquals(30L * 24L * 60L * 60L, secondsBetweenIatAndExp(payload));
    }

    @Test
    void refreshTokenExpirationIsCappedAtThirtyDays() throws Exception {
        JwtService jwt = new JwtService(
                TEST_SECRET,
                "test",
                60,
                999
        );
        jwt.init();

        Map<String, Object> payload = payload(jwt.refresh("user@alumno.ipn.mx"));

        assertEquals(30L * 24L * 60L * 60L, secondsBetweenIatAndExp(payload));
    }

    private Map<String, Object> payload(String jwt) throws Exception {
        String[] parts = jwt.split("\\.");
        String json = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        return mapper.readValue(json, new TypeReference<>() {});
    }

    private long secondsBetweenIatAndExp(Map<String, Object> payload) {
        long iat = ((Number) payload.get("iat")).longValue();
        long exp = ((Number) payload.get("exp")).longValue();
        return exp - iat;
    }
}
