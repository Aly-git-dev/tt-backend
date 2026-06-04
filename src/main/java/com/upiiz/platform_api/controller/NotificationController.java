package com.upiiz.platform_api.controller;

import com.upiiz.platform_api.dto.NotificationResponse;
import com.upiiz.platform_api.entities.User;
import com.upiiz.platform_api.repositories.UserRepository;
import com.upiiz.platform_api.services.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/upiiz/public/v1/notifications")
@Tag(name = "Notificaciones", description = "Consulta y lectura de notificaciones del usuario autenticado")
@SecurityRequirement(name = "bearer-jwt")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public NotificationController(NotificationService notificationService,
                                  UserRepository userRepository) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    @GetMapping
    @Operation(summary = "Listar notificaciones", description = "Devuelve notificaciones paginadas del usuario actual.")
    public Page<NotificationResponse> listMine(
            Authentication authentication,
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            Pageable pageable
    ) {
        UUID userId = resolveUserId(authentication);
        return notificationService.listMine(userId, unreadOnly, pageable);
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Contar no leidas", description = "Devuelve el numero de notificaciones no leidas.")
    public Map<String, Long> unreadCount(Authentication authentication) {
        UUID userId = resolveUserId(authentication);
        return Map.of("count", notificationService.unreadCount(userId));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Marcar notificacion como leida", description = "Marca una notificacion propia como leida.")
    public NotificationResponse markAsRead(
            Authentication authentication,
            @PathVariable UUID id
    ) {
        UUID userId = resolveUserId(authentication);
        return notificationService.markAsRead(userId, id);
    }

    @PatchMapping("/read-all")
    @Operation(summary = "Marcar todas como leidas", description = "Marca todas las notificaciones propias como leidas.")
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
