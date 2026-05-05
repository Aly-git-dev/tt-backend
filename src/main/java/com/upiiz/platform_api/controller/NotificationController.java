package com.upiiz.platform_api.controller;

import com.upiiz.platform_api.dto.NotificationResponse;
import com.upiiz.platform_api.entities.User;
import com.upiiz.platform_api.repositories.UserRepository;
import com.upiiz.platform_api.services.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/upiiz/public/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public NotificationController(NotificationService notificationService,
                                  UserRepository userRepository) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public Page<NotificationResponse> listMine(
            Authentication authentication,
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            Pageable pageable
    ) {
        UUID userId = resolveUserId(authentication);
        return notificationService.listMine(userId, unreadOnly, pageable);
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount(Authentication authentication) {
        UUID userId = resolveUserId(authentication);
        return Map.of("count", notificationService.unreadCount(userId));
    }

    @PatchMapping("/{id}/read")
    public NotificationResponse markAsRead(
            Authentication authentication,
            @PathVariable UUID id
    ) {
        UUID userId = resolveUserId(authentication);
        return notificationService.markAsRead(userId, id);
    }

    @PatchMapping("/read-all")
    public Map<String, String> markAllAsRead(Authentication authentication) {
        UUID userId = resolveUserId(authentication);
        notificationService.markAllAsRead(userId);
        return Map.of("message", "Todas las notificaciones fueron marcadas como leídas");
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