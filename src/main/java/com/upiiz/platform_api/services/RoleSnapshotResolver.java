package com.upiiz.platform_api.services;

import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class RoleSnapshotResolver {

    private final com.upiiz.platform_api.repositories.UserRoleNativeRepo userRoleRepo;

    public RoleSnapshotResolver(com.upiiz.platform_api.repositories.UserRoleNativeRepo userRoleRepo) {
        this.userRoleRepo = userRoleRepo;
    }

    // prioridad (ajusta si quieres)
    private static final List<String> PRIORITY = List.of("ADMIN", "PROFESOR", "ASESOR", "ALUMNO");

    public String primaryRoleOf(UUID userId) {
        List<String> roles;
        roles = userRoleRepo.roleNames(userId);
        if (roles == null || roles.isEmpty()) return null;

        Set<String> set = new HashSet<>();
        for (String r : roles) if (r != null) set.add(r.trim().toUpperCase());

        for (String p : PRIORITY) if (set.contains(p)) return p;
        // fallback al primero
        return roles.get(0).trim().toUpperCase();
    }
}

