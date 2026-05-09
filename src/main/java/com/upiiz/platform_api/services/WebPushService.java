package com.upiiz.platform_api.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.upiiz.platform_api.dto.PushNotificationPayload;
import com.upiiz.platform_api.dto.PushSubscriptionRequest;
import com.upiiz.platform_api.entities.PushSubscription;
import com.upiiz.platform_api.repositories.PushSubscriptionRepository;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.apache.http.HttpResponse;
import org.jose4j.lang.JoseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
public class WebPushService {

    private final PushSubscriptionRepository subscriptionRepo;
    private final ObjectMapper objectMapper;

    private final String publicKey;
    private final String privateKey;
    private final String subject;

    public WebPushService(
            PushSubscriptionRepository subscriptionRepo,
            ObjectMapper objectMapper,
            @Value("${app.push.public-key}") String publicKey,
            @Value("${app.push.private-key}") String privateKey,
            @Value("${app.push.subject}") String subject
    ) {
        this.subscriptionRepo = subscriptionRepo;
        this.objectMapper = objectMapper;
        this.subject = subject == null ? "" : subject.trim();

        String configuredPublicKey = normalizeKey(publicKey);
        String configuredPrivateKey = normalizeKey(privateKey);

        if (isValidPublicKey(configuredPublicKey) && isValidPrivateKey(configuredPrivateKey)) {
            this.publicKey = configuredPublicKey;
            this.privateKey = configuredPrivateKey;
        } else {
            KeyPair generated = generateVapidKeyPair();
            this.publicKey = encodePublicKey(generated);
            this.privateKey = encodePrivateKey(generated);
        }
    }

    public String getPublicKey() {
        return publicKey;
    }

    public boolean hasValidPublicKey() {
        return isValidPublicKey(publicKey);
    }

    public boolean hasValidPrivateKey() {
        return isValidPrivateKey(privateKey);
    }

    @Transactional
    public void saveSubscription(UUID userId, PushSubscriptionRequest request, String userAgent) {
        PushSubscription sub = subscriptionRepo.findByEndpoint(request.endpoint())
                .orElseGet(PushSubscription::new);

        sub.setUserId(userId);
        sub.setEndpoint(request.endpoint());
        sub.setP256dh(request.keys().p256dh());
        sub.setAuth(request.keys().auth());
        sub.setUserAgent(userAgent);
        sub.setActive(true);

        subscriptionRepo.save(sub);
    }

    public void sendToUser(UUID userId, PushNotificationPayload payload) {
        if (!hasValidPublicKey() || !hasValidPrivateKey() || subject.isBlank()) {
            return;
        }

        List<PushSubscription> subscriptions = subscriptionRepo.findByUserIdAndActiveTrue(userId);

        subscriptions.forEach(sub -> {
            try {
                send(sub, payload);
            } catch (Exception e) {
                sub.setActive(false);
                subscriptionRepo.save(sub);
            }
        });
    }

    private void send(PushSubscription sub, PushNotificationPayload payload)
            throws GeneralSecurityException, JoseException, Exception {

        String jsonPayload = objectMapper.writeValueAsString(payload);

        PushService pushService = new PushService(publicKey, privateKey, subject);

        Notification notification = new Notification(
                sub.getEndpoint(),
                sub.getP256dh(),
                sub.getAuth(),
                jsonPayload
        );

        HttpResponse response = pushService.send(notification);

        int statusCode = response.getStatusLine().getStatusCode();

        if (statusCode == 404 || statusCode == 410) {
            sub.setActive(false);
            subscriptionRepo.save(sub);
        }
    }

    private String normalizeKey(String key) {
        return key == null ? "" : key.trim();
    }

    private boolean isValidPublicKey(String key) {
        byte[] decoded = decodeBase64Url(key);
        return decoded.length == 65 && decoded[0] == 0x04;
    }

    private boolean isValidPrivateKey(String key) {
        return decodeBase64Url(key).length == 32;
    }

    private KeyPair generateVapidKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
            generator.initialize(new ECGenParameterSpec("secp256r1"));
            return generator.generateKeyPair();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo generar un par VAPID temporal", e);
        }
    }

    private String encodePublicKey(KeyPair keyPair) {
        ECPublicKey publicKey = (ECPublicKey) keyPair.getPublic();
        byte[] x = unsignedFixedLength(publicKey.getW().getAffineX(), 32);
        byte[] y = unsignedFixedLength(publicKey.getW().getAffineY(), 32);
        byte[] uncompressed = new byte[65];
        uncompressed[0] = 0x04;
        System.arraycopy(x, 0, uncompressed, 1, 32);
        System.arraycopy(y, 0, uncompressed, 33, 32);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(uncompressed);
    }

    private String encodePrivateKey(KeyPair keyPair) {
        ECPrivateKey privateKey = (ECPrivateKey) keyPair.getPrivate();
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(unsignedFixedLength(privateKey.getS(), 32));
    }

    private byte[] unsignedFixedLength(BigInteger value, int length) {
        byte[] source = value.toByteArray();
        byte[] result = new byte[length];
        int copyStart = Math.max(0, source.length - length);
        int copyLength = Math.min(source.length, length);
        System.arraycopy(source, copyStart, result, length - copyLength, copyLength);
        return result;
    }

    private byte[] decodeBase64Url(String value) {
        if (value == null || value.isBlank()) {
            return new byte[0];
        }

        String normalized = value.trim()
                .replace('-', '+')
                .replace('_', '/');
        int padding = (4 - normalized.length() % 4) % 4;
        normalized = normalized + "=".repeat(padding);

        try {
            return Base64.getDecoder().decode(normalized);
        } catch (IllegalArgumentException e) {
            return new byte[0];
        }
    }
}
