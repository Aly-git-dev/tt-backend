package com.upiiz.platform_api.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.upiiz.platform_api.entities.User;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JaasJwtServiceTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void createsJaasJwtWithExpectedHeaderAndClaims() throws Exception {
        String appId = "vpaas-magic-cookie-test";
        JaasJwtService service = new JaasJwtService(
                true,
                "8x8.vc",
                appId,
                "key123",
                privateKeyPem(),
                120,
                "https://api.example/"
        );
        service.init();
        User user = User.builder()
                .id(UUID.randomUUID())
                .emailInst("ana@alumno.ipn.mx")
                .nombre("Ana Lopez")
                .avatarUrl("https://app.example/avatar.png")
                .build();

        String token = service.createToken(user, true, "room-1");

        Map<String, Object> header = part(token, 0);
        Map<String, Object> payload = part(token, 1);
        Map<String, Object> context = map(payload.get("context"));
        Map<String, Object> userClaims = map(context.get("user"));

        assertEquals("RS256", header.get("alg"));
        assertEquals(appId + "/key123", header.get("kid"));
        assertEquals("JWT", header.get("typ"));
        assertEquals("jitsi", payload.get("aud"));
        assertEquals("chat", payload.get("iss"));
        assertEquals(appId, payload.get("sub"));
        assertEquals("*", payload.get("room"));
        assertEquals(user.getId().toString(), userClaims.get("id"));
        assertEquals("Ana Lopez", userClaims.get("name"));
        assertEquals("https://app.example/avatar.png", userClaims.get("avatar"));
        assertEquals("ana@alumno.ipn.mx", userClaims.get("email"));
        assertEquals("true", userClaims.get("moderator"));
        assertNotNull(payload.get("nbf"));
        assertTrue(((Number) payload.get("exp")).longValue() > ((Number) payload.get("nbf")).longValue());
    }

    private String privateKeyPem() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        String body = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.UTF_8))
                .encodeToString(keyPair.getPrivate().getEncoded());

        return "-----BEGIN PRIVATE KEY-----\n" + body + "\n-----END PRIVATE KEY-----";
    }

    private Map<String, Object> part(String jwt, int index) throws Exception {
        String[] parts = jwt.split("\\.");
        String json = new String(Base64.getUrlDecoder().decode(parts[index]), StandardCharsets.UTF_8);
        return mapper.readValue(json, new TypeReference<>() {});
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value) {
        return (Map<String, Object>) value;
    }
}
