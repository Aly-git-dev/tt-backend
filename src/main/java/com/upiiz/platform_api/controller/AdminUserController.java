package com.upiiz.platform_api.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.upiiz.platform_api.dto.UserDTO;
import com.upiiz.platform_api.entities.User;
import com.upiiz.platform_api.repositories.UserRepository;
import com.upiiz.platform_api.services.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@CrossOrigin(origins = "http://148.204.142.20:3030")
@RestController
@RequestMapping("/upiiz/admin/v1")
@Tag(
        name = "Administración de usuarios",
        description = "Endpoints para listar y gestionar solicitudes de aprobación de usuarios"
)
public class AdminUserController {

    private final UserRepository users;
    private final ObjectMapper om;
    private final AuthService authService;

    public AdminUserController(UserRepository users, ObjectMapper om, AuthService authService) {
        this.users = users;
        this.om = om;
        this.authService = authService;
    }

    // ---------------- LISTAR PENDIENTES ----------------
    @GetMapping("/pending-users")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Listar usuarios pendientes de aprobación",
            description = "Devuelve los usuarios que ya confirmaron correo pero aún no han sido aprobados por un administrador."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Listado obtenido correctamente",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserDTO.class),
                            examples = @ExampleObject(
                                    name = "Listado de pendientes",
                                    value = "{\n" +
                                            "  \"estado\": 1,\n" +
                                            "  \"mensaje\": \"Pendientes obtenidos correctamente\",\n" +
                                            "  \"usuarios\": [\n" +
                                            "    {\n" +
                                            "      \"id\": \"f6c6c4bb-5320-4f5f-9e21-8d2e1df3a012\",\n" +
                                            "      \"emailInst\": \"user@upiiz.ipn.mx\",\n" +
                                            "      \"fullName\": \"Nombre Apellido\",\n" +
                                            "      \"active\": true,\n" +
                                            "      \"bio\": null,\n" +
                                            "      \"interests\": [],\n" +
                                            "      \"links\": [],\n" +
                                            "      \"avatarUrl\": null,\n" +
                                            "      \"coverUrl\": null,\n" +
                                            "      \"roles\": [\"ALUMNO\"]\n" +
                                            "    }\n" +
                                            "  ]\n" +
                                            "}"
                            )
                    )
            )
    })
    public ResponseEntity<?> pendingUsers() {
        try {
            List<User> pending = users
                    .findByEmailVerifiedTrueAndApprovedFalseAndActiveTrue();

            List<UserDTO> dtos = pending.stream()
                    .map(u -> {
                        try {
                            return toDTO(u);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .toList();

            return ResponseEntity.ok(
                    Map.of(
                            "estado", 1,
                            "mensaje", "Pendientes obtenidos correctamente",
                            "usuarios", dtos
                    )
            );
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    Map.of(
                            "estado", 0,
                            "mensaje", "Error al obtener pendientes",
                            "error", e.getMessage()
                    )
            );
        }
    }

    // ---------------- APROBAR (envoltorio al service existente) ----------------
    @PatchMapping("/pending-users/{userId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Aprobar usuario pendiente",
            description = "Marca como aprobado a un usuario que estaba pendiente."
    )
    public ResponseEntity<?> approveUser(
            @PathVariable UUID userId
    ) {
        try {
            // reutilizamos tu lógica de AuthService
            var res = authService.approveUser(userId);
            return ResponseEntity.ok(
                    Map.of(
                            "estado", 1,
                            "mensaje", "Usuario aprobado correctamente",
                            "detalle", res
                    )
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(
                    Map.of("estado", 0, "mensaje", e.getMessage())
            );
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(
                    Map.of("estado", 0, "mensaje", e.getMessage())
            );
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    Map.of(
                            "estado", 0,
                            "mensaje", "Error al aprobar usuario",
                            "error", e.getMessage()
                    )
            );
        }
    }

    // ---------------- RECHAZAR ----------------
    @PatchMapping("/pending-users/{userId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Rechazar solicitud de un usuario",
            description = "Marca un usuario pendiente como rechazado para que ya no aparezca en el listado."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Solicitud rechazada correctamente",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Rechazo exitoso",
                                    value = "{\n" +
                                            "  \"estado\": 1,\n" +
                                            "  \"mensaje\": \"Solicitud rechazada\"\n" +
                                            "}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Usuario no encontrado"
            )
    })
    public ResponseEntity<?> rejectUser(@PathVariable UUID userId) {
        try {
            var u = users.findById(userId).orElse(null);
            if (u == null) {
                return ResponseEntity.status(404).body(
                        Map.of("estado", 0, "mensaje", "Usuario no encontrado")
                );
            }

            // sólo permitimos rechazar si realmente está pendiente
            if (!u.isEmailVerified() || u.isApproved() || !u.isActive()) {
                return ResponseEntity.status(409).body(
                        Map.of("estado", 0, "mensaje", "El usuario no está en estado pendiente")
                );
            }

            // opción sencilla: lo desactivamos
            u.setActive(false);
            users.save(u);

            return ResponseEntity.ok(
                    Map.of("estado", 1, "mensaje", "Solicitud rechazada")
            );
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    Map.of(
                            "estado", 0,
                            "mensaje", "Error al rechazar usuario",
                            "error", e.getMessage()
                    )
            );
        }
    }

    // ------------ misma conversión que usas en MeController ------------
    private UserDTO toDTO(User u) throws Exception {
        List<String> interests = u.getInterests() == null ? List.of() :
                om.readValue(u.getInterests(), new TypeReference<List<String>>() {});
        List<UserDTO.LinkDTO> links = u.getLinks() == null ? List.of() :
                om.readValue(u.getLinks(), new TypeReference<List<UserDTO.LinkDTO>>() {});
        List<String> roles = u.getRoles().stream().map(r -> r.getName()).toList();

        return new UserDTO(
                u.getId(),
                u.getEmailInst(),
                u.getNombre(),
                u.isActive(),
                u.getBio(),
                interests,
                links,
                u.getAvatarUrl(),
                u.getCoverUrl(),
                roles,
                u.getBoleta(),
                u.getCarrera()
        );
    }
}
