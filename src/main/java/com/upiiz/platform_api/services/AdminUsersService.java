package com.upiiz.platform_api.services;

import com.upiiz.platform_api.dto.AdminUserDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

@Service
public class AdminUsersService {

    private final JdbcTemplate jdbc;

    public AdminUsersService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<AdminUserDto> listUsers(String q) {
        String term = q == null ? "" : q.trim().toLowerCase();

        String sql = """
            SELECT id, email_inst, full_name, active, approved, email_verified, created_at
            FROM users
            WHERE LOWER(COALESCE(email_inst, '')) LIKE ?
               OR LOWER(COALESCE(full_name, '')) LIKE ?
            ORDER BY created_at DESC
        """;

        String like = "%" + term + "%";

        List<AdminUserDto> users = jdbc.query(sql, (rs, rowNum) -> {
            AdminUserDto u = new AdminUserDto();
            u.id = UUID.fromString(rs.getString("id"));
            u.emailInst = rs.getString("email_inst");
            u.fullName = rs.getString("full_name");
            u.active = rs.getBoolean("active");
            u.approved = getBooleanSafe(rs, "approved");
            u.emailVerified = getBooleanSafe(rs, "email_verified");

            Timestamp ts = rs.getTimestamp("created_at");
            u.createdAt = ts != null ? ts.toInstant() : null;

            return u;
        }, like, like);

        users.forEach(u -> u.roles = rolesOf(u.id));

        return users;
    }

    @Transactional
    public AdminUserDto updateRoles(UUID userId, List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            throw new IllegalArgumentException("Debe asignarse al menos un rol");
        }

        List<String> cleanRoles = roles.stream()
                .filter(Objects::nonNull)
                .map(r -> r.trim().toUpperCase())
                .filter(r -> !r.isBlank())
                .distinct()
                .toList();

        if (cleanRoles.isEmpty()) {
            throw new IllegalArgumentException("Roles inválidos");
        }

        jdbc.update("DELETE FROM user_roles WHERE user_id = ?", userId);

        for (String role : cleanRoles) {
            Integer roleId = jdbc.queryForObject(
                    "SELECT id FROM roles WHERE UPPER(name) = UPPER(?)",
                    Integer.class,
                    role
            );

            jdbc.update(
                    "INSERT INTO user_roles(user_id, role_id) VALUES (?, ?)",
                    userId,
                    roleId
            );
        }

        return findById(userId);
    }

    @Transactional
    public AdminUserDto updateActive(UUID userId, boolean active) {
        jdbc.update("""
            UPDATE users
            SET active = ?, updated_at = NOW()
            WHERE id = ?
        """, active, userId);

        return findById(userId);
    }

    public AdminUserDto findById(UUID id) {
        List<AdminUserDto> users = jdbc.query("""
            SELECT id, email_inst, full_name, active, approved, email_verified, created_at
            FROM users
            WHERE id = ?
        """, (rs, rowNum) -> {
            AdminUserDto u = new AdminUserDto();
            u.id = UUID.fromString(rs.getString("id"));
            u.emailInst = rs.getString("email_inst");
            u.fullName = rs.getString("full_name");
            u.active = rs.getBoolean("active");
            u.approved = getBooleanSafe(rs, "approved");
            u.emailVerified = getBooleanSafe(rs, "email_verified");

            Timestamp ts = rs.getTimestamp("created_at");
            u.createdAt = ts != null ? ts.toInstant() : null;

            u.roles = rolesOf(u.id);
            return u;
        }, id);

        if (users.isEmpty()) {
            throw new IllegalArgumentException("Usuario no encontrado");
        }

        return users.get(0);
    }

    private List<String> rolesOf(UUID userId) {
        return jdbc.queryForList("""
            SELECT r.name
            FROM roles r
            JOIN user_roles ur ON ur.role_id = r.id
            WHERE ur.user_id = ?
            ORDER BY r.name
        """, String.class, userId);
    }

    private Boolean getBooleanSafe(java.sql.ResultSet rs, String column) {
        try {
            return rs.getBoolean(column);
        } catch (Exception e) {
            return null;
        }
    }
}