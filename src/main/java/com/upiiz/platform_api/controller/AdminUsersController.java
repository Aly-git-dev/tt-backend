package com.upiiz.platform_api.controller;

import com.upiiz.platform_api.dto.AdminUserDto;
import com.upiiz.platform_api.dto.UpdateUserActiveRequest;
import com.upiiz.platform_api.dto.UpdateUserRolesRequest;
import com.upiiz.platform_api.security.CurrentUser;
import com.upiiz.platform_api.services.AdminUsersService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/upiiz/admin/v1/admin/users")
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
    public List<AdminUserDto> listUsers(
            @RequestParam(required = false, defaultValue = "") String q
    ) {
        requireAdmin();
        return service.listUsers(q);
    }

    @PatchMapping("/{id}/roles")
    public AdminUserDto updateRoles(
            @PathVariable UUID id,
            @RequestBody UpdateUserRolesRequest req
    ) {
        requireAdmin();
        return service.updateRoles(id, req.roles);
    }

    @PatchMapping("/{id}/active")
    public AdminUserDto updateActive(
            @PathVariable UUID id,
            @RequestBody UpdateUserActiveRequest req
    ) {
        requireAdmin();
        return service.updateActive(id, Boolean.TRUE.equals(req.active));
    }

    @PatchMapping("/{id}/ban")
    public AdminUserDto ban(@PathVariable UUID id) {
        requireAdmin();
        return service.updateActive(id, false);
    }

    @PatchMapping("/{id}/unban")
    public AdminUserDto unban(@PathVariable UUID id) {
        requireAdmin();
        return service.updateActive(id, true);
    }
}