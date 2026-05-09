package com.upiiz.platform_api.controller;

import com.upiiz.platform_api.dto.PushSubscriptionRequest;
import com.upiiz.platform_api.entities.User;
import com.upiiz.platform_api.repositories.UserRepository;
import com.upiiz.platform_api.services.WebPushService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/upiiz/public/v1/push")
public class PushSubscriptionController {

    private final WebPushService webPushService;
    private final UserRepository userRepository;

    public PushSubscriptionController(WebPushService webPushService,
                                      UserRepository userRepository) {
        this.webPushService = webPushService;
        this.userRepository = userRepository;
    }

    @GetMapping("/public-key")
    public ResponseEntity<Map<String, String>> publicKey() {
        return ResponseEntity.ok(Map.of("publicKey", webPushService.getPublicKey()));
    }

    @PostMapping("/subscribe")
    public Map<String, String> subscribe(
            Authentication authentication,
            @RequestBody PushSubscriptionRequest request,
            HttpServletRequest servletRequest
    ) {
        UUID userId = resolveUserId(authentication);
        String userAgent = servletRequest.getHeader("User-Agent");

        webPushService.saveSubscription(userId, request, userAgent);

        return Map.of("message", "Suscripción push guardada correctamente");
    }

    private UUID resolveUserId(Authentication authentication) {
        String principal = authentication.getName();

        try {
            return UUID.fromString(principal);
        } catch (IllegalArgumentException ignored) {
            User user = userRepository.findByEmailInst(principal)
                    .orElseThrow(() -> new IllegalArgumentException("Usuario autenticado no encontrado"));
            return user.getId();
        }
    }
}
