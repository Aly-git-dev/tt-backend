package com.upiiz.platform_api.services;

import com.upiiz.platform_api.auth.dto.LoginRequest;
import com.upiiz.platform_api.auth.dto.RegisterRequest;
import com.upiiz.platform_api.auth.dto.TokensResponse;
import com.upiiz.platform_api.entities.EmailVerification;
import com.upiiz.platform_api.entities.Role;
import com.upiiz.platform_api.entities.User;
import com.upiiz.platform_api.repositories.EmailVerificationRepository;
import com.upiiz.platform_api.repositories.RoleRepository;
import com.upiiz.platform_api.repositories.UserRepository;
import com.upiiz.platform_api.gesco.GescoClient; // GESCO: quita este import si no lo usas
import com.upiiz.platform_api.gesco.GescoLoginResponse; // GESCO: idem
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class AuthService {

    private final AuthenticationManager authManager;
    private final UserRepository users;
    private final RoleRepository roles;
    private final EmailVerificationRepository emailVerifs;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final MailService mail;
    private final GescoClient gesco;

    public AuthService(AuthenticationManager authManager,
                       UserRepository users,
                       RoleRepository roles,
                       EmailVerificationRepository emailVerifs,
                       PasswordEncoder encoder,
                       JwtService jwt,
                       MailService mail,
                       GescoClient gesco
    ) {
        this.authManager = authManager;
        this.users = users;
        this.roles = roles;
        this.emailVerifs = emailVerifs;
        this.encoder = encoder;
        this.jwt = jwt;
        this.mail = mail;
        this.gesco = gesco; // GESCO
    }

    // =============== REGISTRO LOCAL ===============
    @Transactional
    public Map<String, Object> register(RegisterRequest r, String appBaseUrl) {
        if (r.getEmailInst() == null || !r.getEmailInst().toLowerCase().contains("ipn")) {
            throw new IllegalArgumentException("El correo debe ser institucional (@ipn)");
        }
        if (r.getPassword() == null || r.getPassword().length() < 6) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 6 caracteres");
        }
        users.findByEmailInst(r.getEmailInst()).ifPresent(u -> {
            throw new IllegalStateException("El correo ya está registrado");
        });

        String roleName = (r.getRole() == null || r.getRole().isBlank()) ? "PROFESOR" : r.getRole().toUpperCase();
        Role role = roles.findByName(roleName);
        if (role == null) throw new IllegalArgumentException("Rol inválido: " + roleName);

        var u = new User();
        u.setEmailInst(r.getEmailInst());
        u.setNombre(r.getFullName());
        u.setPasswordHash(encoder.encode(r.getPassword()));
        u.setActive(true);
        u.setAuthProvider("LOCAL");
        u.setEmailVerified(false);
        u.setApproved(false);
        u.setRoles(Set.of(role));
        users.save(u);

        var ev = new EmailVerification();
        ev.setUserId(u.getId());
        ev.setExpiresAt(Instant.now().plus(48, ChronoUnit.HOURS));
        emailVerifs.save(ev);

        String link = appBaseUrl + "/upiiz/public/v1/auth/confirm?token=" + ev.getToken();
        mail.sendVerificationEmail(u.getEmailInst(), link);

        return Map.of("estado", 1, "mensaje", "Registro creado. Revisa tu correo para confirmar.");
    }

    // =============== CONFIRMAR CORREO ===============
    @Transactional
    public Map<String, Object> confirmEmail(UUID token) {
        var ev = emailVerifs.findById(token).orElseThrow(() -> new IllegalArgumentException("Token inválido"));
        if (ev.isUsed()) throw new IllegalStateException("Token ya utilizado");
        if (ev.getExpiresAt().isBefore(Instant.now())) throw new IllegalStateException("Token expirado");

        var u = users.findById(ev.getUserId()).orElseThrow();
        u.setEmailVerified(true);
        users.save(u);

        ev.setUsed(true);
        emailVerifs.save(ev);

        return Map.of("estado", 1, "mensaje", "Correo confirmado. Espera la aprobación del administrador.");
    }

    // =============== APROBAR USUARIO (ADMIN) ===============
    @Transactional
    public Map<String, Object> approveUser(UUID userId) {
        var u = users.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        if (!u.isActive()) {
            throw new IllegalStateException("El usuario esta desactivado");
        }
        if (!u.isEmailVerified()) {
            throw new IllegalStateException("El usuario aun no confirma su correo");
        }
        u.setApproved(true);
        users.save(u);
        return Map.of("estado", 1, "mensaje", "Usuario aprobado");
    }

    // =============== LOGIN HÍBRIDO ===============
    public TokensResponse login(LoginRequest req) {
        if (req.username() == null || req.password() == null) {
            throw new RuntimeException("Faltan credenciales");
        }

        // 1) LOCAL por email
        if (req.username().contains("@")) {
            var local = users.findByEmailInst(req.username())
                    .filter(u -> u.getAuthProvider().equals("LOCAL"))
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            // valida contraseña
            authManager.authenticate(new UsernamePasswordAuthenticationToken(req.username(), req.password()));
            // candados
            if (!local.isEmailVerified()) throw new RuntimeException("Debes confirmar tu correo.");
            if (!local.isApproved()) throw new RuntimeException("Pendiente aprobación del administrador.");
            return issue(local);
        }

        // 2) GESCO por boleta/usuario (si tu identifier no tiene @)
        // GESCO: descomenta si tienes el cliente funcionando
        var resp = gesco.login(req.username(), req.password());
        if (resp == null || !resp.isEstatus()) throw new RuntimeException("Credenciales inválidas");

        GescoLoginResponse.Datos d = resp.getDatos();
        var u = users.findByExternalId(d.getBoleta()).orElse(null);
        if (u == null) {
            // por defecto ALUMNO
            var role = roles.findByName("ALUMNO");
            u = new User();
            u.setExternalId(d.getBoleta());
            u.setEmailInst(d.getMail());
            u.setNombre(d.getNombre());
            u.setCarrera(d.getCarrera());
            u.setActive(true);
            u.setAuthProvider("GESCO");
            u.setRoles(Set.of(role));
        } else {
            u.setEmailInst(d.getMail());
            u.setNombre(d.getNombre());
            u.setCarrera(d.getCarrera());
            u.setActive(true);
            u.setAuthProvider("GESCO");
        }
        users.save(u);
        return issue(u);
    }

    // =============== REFRESH ===============
    public TokensResponse refresh(String refreshToken) {
        var email = jwt.subject(refreshToken); // subject = emailInst
        var u = users.findByEmailInst(email).orElseThrow();
        return issue(u, refreshToken);
    }

    // =============== Helpers JWT ===============
    private TokensResponse issue(User u) {
        return issue(u, jwt.refresh(u.getEmailInst()));
    }

    private TokensResponse issue(User u, String refresh) {
        var claims = new HashMap<String, Object>();
        claims.put("uid", String.valueOf(u.getId()));
        claims.put("roles", u.getRoles().stream().map(Role::getName).toArray(String[]::new));
        claims.put("prov", u.getAuthProvider());
        return new TokensResponse(jwt.access(claims, u.getEmailInst()), refresh);
    }
}
