package com.upiiz.platform_api.controller;

import com.upiiz.platform_api.dto.AdminUserDto;
import com.upiiz.platform_api.dto.UpdateUserActiveRequest;
import com.upiiz.platform_api.dto.UpdateUserRolesRequest;
import com.upiiz.platform_api.security.CurrentUser;
import com.upiiz.platform_api.services.AdminUsersService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/upiiz/admin/v1/admin/users")
@Tag(name = "Admin - usuarios", description = "Gestion de usuarios, roles y estado activo")
@SecurityRequirement(name = "bearer-jwt")
public class AdminUsersController {

    private final AdminUsersService service;

    public AdminUsersController(AdminUsersService service) {
        this.service = service;
    }

    private void requireAdmin() {
        if (!CurrentUser.hasRole("ADMIN")) {
            throw new SecurityException("ADMIN required");
        }
    }

    @GetMapping
    @Operation(summary = "Listar usuarios", description = "Busca usuarios por texto y devuelve sus datos administrativos.")
    public List<AdminUserDto> listUsers(
            @RequestParam(required = false, defaultValue = "") String q
    ) {
        requireAdmin();
        return service.listUsers(q);
    }

    @PatchMapping("/{id}/roles")
    @Operation(summary = "Actualizar roles", description = "Reemplaza los roles asignados a un usuario.")
    public AdminUserDto updateRoles(
            @PathVariable UUID id,
            @RequestBody UpdateUserRolesRequest req
    ) {
        requireAdmin();
        return service.updateRoles(id, req.roles);
    }

    @PatchMapping("/{id}/active")
    @Operation(summary = "Actualizar estado activo", description = "Activa o desactiva una cuenta de usuario.")
    public AdminUserDto updateActive(
            @PathVariable UUID id,
            @RequestBody UpdateUserActiveRequest req
    ) {
        requireAdmin();
        return service.updateActive(id, Boolean.TRUE.equals(req.active));
    }

    @PatchMapping("/{id}/ban")
    @Operation(summary = "Banear usuario", description = "Desactiva la cuenta de un usuario.")
    public AdminUserDto ban(@PathVariable UUID id) {
        requireAdmin();
        return service.updateActive(id, false);
    }

    @PatchMapping("/{id}/unban")
    @Operation(summary = "Quitar baneo", description = "Reactiva la cuenta de un usuario previamente desactivado.")
    public AdminUserDto unban(@PathVariable UUID id) {
        requireAdmin();
        return service.updateActive(id, true);
    }
}
