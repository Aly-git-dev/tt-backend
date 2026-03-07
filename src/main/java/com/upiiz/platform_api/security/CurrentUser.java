package com.upiiz.platform_api.security;

import com.upiiz.platform_api.repositories.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CurrentUser {

    private static UserRepository users = null;

    public CurrentUser(UserRepository users) {
        this.users = users;
    }

    public static UUID id() {
        var email = email(); // auth.getName() = email
        return users.findByEmailInst(email)
                .map(u -> u.getId())
                .orElseThrow(() -> new RuntimeException("Unauthorized"));
    }

    public static String email() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) throw new RuntimeException("Unauthorized");
        return auth.getName();
    }

    public static boolean hasRole(String roleName) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + roleName) || a.getAuthority().equals(roleName));
    }
}