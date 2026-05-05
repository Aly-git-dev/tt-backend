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

import java.security.GeneralSecurityException;
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
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.subject = subject;
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
}