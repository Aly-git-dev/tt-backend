package com.upiiz.platform_api.services;

import com.upiiz.platform_api.entities.User;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class JaasJwtService {

    private static final long MAX_TOKEN_TTL_MINUTES = 120;

    private final boolean enabled;
    private final String domain;
    private final String appId;
    private final String keyId;
    private final String privateKeyValue;
    private final long tokenTtlMinutes;
    private final String publicBaseUrl;

    private PrivateKey privateKey;

    public JaasJwtService(
            @Value("${app.video.jaas.enabled:false}") boolean enabled,
            @Value("${app.video.jaas.domain:8x8.vc}") String domain,
            @Value("${app.video.jaas.app-id:}") String appId,
            @Value("${app.video.jaas.key-id:}") String keyId,
            @Value("${app.video.jaas.private-key:}") String privateKeyValue,
            @Value("${app.video.jaas.token-ttl-min:120}") long tokenTtlMinutes,
            @Value("${app.api.url:}") String publicBaseUrl
    ) {
        this.enabled = enabled;
        this.domain = normalize(domain, "8x8.vc");
        this.appId = normalize(appId, "");
        this.keyId = normalize(keyId, "");
        this.privateKeyValue = privateKeyValue == null ? "" : privateKeyValue.trim();
        this.tokenTtlMinutes = Math.min(Math.max(tokenTtlMinutes, 1), MAX_TOKEN_TTL_MINUTES);
        this.publicBaseUrl = stripTrailingSlash(normalize(publicBaseUrl, ""));
    }

    @PostConstruct
    void init() {
        if (!enabled) {
            return;
        }

        if (appId.isBlank() || keyId.isBlank() || privateKeyValue.isBlank()) {
            throw new IllegalStateException("JaaS requiere app-id, key-id y private-key cuando app.video.jaas.enabled=true");
        }

        this.privateKey = parsePrivateKey(privateKeyValue);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String domain() {
        return domain;
    }

    public String appId() {
        return appId;
    }

    public String fullRoomName(String roomName) {
        return appId + "/" + roomName;
    }

    public String meetingUrl(String roomName) {
        return "https://" + domain + "/" + fullRoomName(roomName);
    }

    public String externalApiUrl() {
        return "https://" + domain + "/" + appId + "/external_api.js";
    }

    public String createToken(User user, boolean moderator, String roomName) {
        if (!enabled) {
            return null;
        }

        Instant now = Instant.now();
        Map<String, Object> userClaims = new LinkedHashMap<>();
        userClaims.put("id", user.getId().toString());
        userClaims.put("name", displayName(user));
        userClaims.put("avatar", publicUrl(user.getAvatarUrl()));
        userClaims.put("email", user.getEmailInst() == null ? "" : user.getEmailInst());
        userClaims.put("moderator", Boolean.toString(moderator));

        Map<String, Object> features = new LinkedHashMap<>();
        features.put("livestreaming", false);
        features.put("outbound-call", false);
        features.put("transcription", false);
        features.put("recording", false);

        Map<String, Object> context = new LinkedHashMap<>();
        context.put("user", userClaims);
        context.put("features", features);
        context.put("room", Map.of("regex", false));

        return Jwts.builder()
                .header()
                    .keyId(fullKeyId())
                    .type("JWT")
                    .and()
                .claim("aud", "jitsi")
                .issuer("chat")
                .notBefore(java.util.Date.from(now.minus(10, ChronoUnit.SECONDS)))
                .expiration(java.util.Date.from(now.plus(tokenTtlMinutes, ChronoUnit.MINUTES)))
                .subject(appId)
                .claim("room", "*")
                .claim("context", context)
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    private String fullKeyId() {
        return keyId.contains("/") ? keyId : appId + "/" + keyId;
    }

    private String displayName(User user) {
        return user.getNombre() == null || user.getNombre().isBlank()
                ? "Usuario"
                : user.getNombre();
    }

    private String normalize(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private String stripTrailingSlash(String value) {
        if (value == null || value.isBlank() || !value.endsWith("/")) {
            return value;
        }
        return value.substring(0, value.length() - 1);
    }

    private String publicUrl(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        String trimmed = value.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://") || publicBaseUrl.isBlank()) {
            return trimmed;
        }

        return trimmed.startsWith("/")
                ? publicBaseUrl + trimmed
                : publicBaseUrl + "/" + trimmed;
    }

    private PrivateKey parsePrivateKey(String value) {
        try {
            byte[] keyBytes = extractPrivateKeyBytes(value);
            return generatePrivateKey(keyBytes, false);
        } catch (Exception firstFailure) {
            try {
                byte[] keyBytes = extractPrivateKeyBytes(value);
                return generatePrivateKey(keyBytes, true);
            } catch (Exception secondFailure) {
                throw new IllegalStateException("No se pudo leer la private-key de JaaS", secondFailure);
            }
        }
    }

    private PrivateKey generatePrivateKey(byte[] keyBytes, boolean pkcs1) throws Exception {
        byte[] pkcs8 = pkcs1 ? wrapPkcs1RsaKey(keyBytes) : keyBytes;
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(pkcs8));
    }

    private byte[] extractPrivateKeyBytes(String value) {
        String normalized = value.replace("\\n", "\n").trim();

        if (!normalized.startsWith("-----BEGIN")) {
            byte[] decoded = Base64.getDecoder().decode(normalized.replaceAll("\\s+", ""));
            String decodedText = new String(decoded, StandardCharsets.UTF_8).trim();
            if (decodedText.startsWith("-----BEGIN")) {
                normalized = decodedText;
            } else {
                return decoded;
            }
        }

        String base64 = normalized
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");

        return Base64.getMimeDecoder().decode(base64);
    }

    private byte[] wrapPkcs1RsaKey(byte[] pkcs1) {
        byte[] version = new byte[] {0x02, 0x01, 0x00};
        byte[] algorithm = new byte[] {
                0x30, 0x0d,
                0x06, 0x09,
                0x2a, (byte) 0x86, 0x48, (byte) 0x86, (byte) 0xf7, 0x0d, 0x01, 0x01, 0x01,
                0x05, 0x00
        };
        byte[] privateKey = der(0x04, pkcs1);

        return der(0x30, concat(version, algorithm, privateKey));
    }

    private byte[] der(int tag, byte[] payload) {
        return concat(new byte[] {(byte) tag}, derLength(payload.length), payload);
    }

    private byte[] derLength(int length) {
        if (length < 128) {
            return new byte[] {(byte) length};
        }

        int bytes = 0;
        int value = length;
        while (value > 0) {
            bytes++;
            value >>= 8;
        }

        byte[] encoded = new byte[bytes + 1];
        encoded[0] = (byte) (0x80 | bytes);
        for (int i = bytes; i > 0; i--) {
            encoded[i] = (byte) (length & 0xff);
            length >>= 8;
        }
        return encoded;
    }

    private byte[] concat(byte[]... arrays) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            for (byte[] array : arrays) {
                out.write(array);
            }
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo construir la llave JaaS", e);
        }
    }
}
