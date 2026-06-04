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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class AuthService {

    private final AuthenticationManager authManager;
    private final UserRepository users;
    private final RoleRepository roles;
    private final EmailVerificationRepository emailVerifs;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final MailService mail;
    private final String apiBaseUrl;

    public AuthService(AuthenticationManager authManager,
                       UserRepository users,
                       RoleRepository roles,
                       EmailVerificationRepository emailVerifs,
                       PasswordEncoder encoder,
                       JwtService jwt,
                       MailService mail,
                       @Value("${app.api.url}") String apiBaseUrl
    ) {
        this.authManager = authManager;
        this.users = users;
        this.roles = roles;
        this.emailVerifs = emailVerifs;
        this.encoder = encoder;
        this.jwt = jwt;
        this.mail = mail;
        this.apiBaseUrl = normalizeBaseUrl(apiBaseUrl);
    }

    @Transactional
    public Map<String, Object> register(RegisterRequest r) {
        return register(r, null);
    }

    @Transactional
    public Map<String, Object> register(RegisterRequest r, String verificationBaseUrl) {
        String email = normalizeEmail(r.getEmailInst());
        validateInstitutionalEmail(email);

        if (r.getFullName() == null || r.getFullName().isBlank()) {
            throw new IllegalArgumentException("El nombre completo es obligatorio");
        }
        if (r.getPassword() == null || r.getPassword().length() < 6) {
            throw new IllegalArgumentException("La contrasena debe tener al menos 6 caracteres");
        }

        String roleName = resolveRegistrationRole(r.getRole(), email);
        Role role = roles.findByName(roleName);
        if (role == null) {
            throw new IllegalArgumentException("Rol invalido: " + roleName);
        }

        var existing = users.findByEmailInstIgnoreCase(email).orElse(null);
        if (existing != null) {
            return registerExistingExternalAccount(existing, r, email, role, roleName, verificationBaseUrl);
        }

        var u = new User();
        u.setEmailInst(email);
        u.setNombre(r.getFullName().trim());
        u.setPasswordHash(encoder.encode(r.getPassword()));
        u.setActive(true);
        u.setAuthProvider("LOCAL");
        u.setEmailVerified(false);
        u.setApproved(isAutoApprovedRole(roleName));
        u.setRoles(Set.of(role));
        users.save(u);

        sendNewVerificationEmail(u, verificationBaseUrl);

        return Map.of("estado", 1, "mensaje", "Registro creado. Revisa tu correo para confirmar.");
    }

    private Map<String, Object> registerExistingExternalAccount(
            User existing,
            RegisterRequest request,
            String email,
            Role role,
            String roleName,
            String verificationBaseUrl
    ) {
        if (isLocalUser(existing) && existing.getPasswordHash() != null && !existing.getPasswordHash().isBlank()) {
            throw new IllegalStateException("El correo ya esta registrado");
        }

        existing.setEmailInst(email);
        existing.setNombre(request.getFullName().trim());
        existing.setPasswordHash(encoder.encode(request.getPassword()));
        existing.setActive(true);
        existing.setAuthProvider("LOCAL");
        existing.setEmailVerified(false);
        existing.setApproved(isAutoApprovedRole(roleName));
        if (existing.getRoles() == null || existing.getRoles().isEmpty()) {
            existing.setRoles(Set.of(role));
        }
        users.save(existing);

        invalidatePendingVerificationTokens(existing);
        sendNewVerificationEmail(existing, verificationBaseUrl);

        return Map.of("estado", 1, "mensaje", "Cuenta actualizada. Revisa tu correo para confirmar.");
    }

    @Transactional
    public Map<String, Object> resendVerification(String emailInst) {
        return resendVerification(emailInst, null);
    }

    @Transactional
    public Map<String, Object> resendVerification(String emailInst, String verificationBaseUrl) {
        String email = normalizeEmail(emailInst);
        validateInstitutionalEmail(email);

        User user = users.findByEmailInstIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        if (user.isEmailVerified()) {
            return Map.of("estado", 1, "mensaje", "El correo ya fue confirmado.");
        }

        invalidatePendingVerificationTokens(user);
        sendNewVerificationEmail(user, verificationBaseUrl);

        return Map.of("estado", 1, "mensaje", "Correo de confirmacion reenviado.");
    }

    private void sendNewVerificationEmail(User user, String verificationBaseUrl) {
        var ev = new EmailVerification();
        ev.setUserId(user.getId());
        ev.setExpiresAt(Instant.now().plus(48, ChronoUnit.HOURS));
        EmailVerification saved = emailVerifs.save(ev);

        String baseUrl = normalizeBaseUrl(
                verificationBaseUrl == null || verificationBaseUrl.isBlank()
                        ? apiBaseUrl
                        : verificationBaseUrl
        );
        String link = baseUrl + "/upiiz/public/v1/auth/confirm?token=" + saved.getToken();
        mail.sendVerificationEmail(user.getEmailInst(), link);
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "http://localhost:8080";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    @Transactional
    public Map<String, Object> confirmEmail(UUID token) {
        var ev = emailVerifs.findById(token).orElseThrow(() -> new IllegalArgumentException("Token invalido"));
        if (ev.isUsed()) {
            var user = users.findById(ev.getUserId()).orElse(null);
            if (user != null && user.isEmailVerified()) {
                String message = user.isApproved()
                        ? "Correo confirmado. Ya puedes iniciar sesion."
                        : "Correo confirmado. Espera la aprobacion del administrador.";
                return Map.of("estado", 1, "mensaje", message);
            }
            throw new IllegalStateException("Token ya utilizado");
        }
        if (ev.getExpiresAt().isBefore(Instant.now())) throw new IllegalStateException("Token expirado");

        var u = users.findById(ev.getUserId()).orElseThrow();
        u.setEmailVerified(true);
        if (hasRole(u, "ALUMNO")) {
            u.setApproved(true);
        }
        users.save(u);

        ev.setUsed(true);
        emailVerifs.save(ev);

        String message = u.isApproved()
                ? "Correo confirmado. Ya puedes iniciar sesion."
                : "Correo confirmado. Espera la aprobacion del administrador.";
        return Map.of("estado", 1, "mensaje", message);
    }

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

    public TokensResponse login(LoginRequest req) {
        if (req == null || req.username() == null || req.username().isBlank()
                || req.password() == null || req.password().isBlank()) {
            throw new IllegalArgumentException("Correo y contrasena son obligatorios");
        }

        String email = normalizeEmail(req.username());
        validateInstitutionalEmail(email);

        var local = users.findByEmailInstIgnoreCase(email)
                .filter(this::isLocalUser)
                .filter(u -> u.getPasswordHash() != null && !u.getPasswordHash().isBlank())
                .orElseThrow(() -> new BadCredentialsException("Correo o contrasena incorrectos"));

        try {
            authManager.authenticate(new UsernamePasswordAuthenticationToken(email, req.password()));
        } catch (AuthenticationException ex) {
            throw new BadCredentialsException("Correo o contrasena incorrectos");
        }

        if (!local.isEmailVerified()) throw new IllegalStateException("Debes confirmar tu correo.");
        if (!local.isApproved()) throw new IllegalStateException("Pendiente aprobacion del administrador.");
        return issue(local);
    }

    public TokensResponse refresh(String refreshToken) {
        var email = jwt.subject(refreshToken);
        var u = users.findByEmailInstIgnoreCase(email).orElseThrow();
        return issue(u, refreshToken);
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("El correo institucional es obligatorio");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private void validateInstitutionalEmail(String email) {
        int at = email.indexOf("@");
        if (at <= 0 || at != email.lastIndexOf("@") || at == email.length() - 1) {
            throw new IllegalArgumentException("El correo institucional no es valido");
        }

        String domain = email.substring(at + 1);
        if (!domain.equals("ipn.mx") && !domain.equals("alumno.ipn.mx")) {
            throw new IllegalArgumentException("El correo debe ser institucional (@ipn.mx o @alumno.ipn.mx)");
        }
    }

    private String resolveRegistrationRole(String requestedRole, String email) {
        boolean alumnoEmail = isAlumnoEmail(email);
        if (requestedRole == null || requestedRole.isBlank()) {
            return alumnoEmail ? "ALUMNO" : "PROFESOR";
        }

        String roleName = requestedRole.trim().toUpperCase(Locale.ROOT);
        if (alumnoEmail && !"ALUMNO".equals(roleName)) {
            throw new IllegalArgumentException("Los correos @alumno.ipn.mx solo pueden registrarse como ALUMNO");
        }
        if (!alumnoEmail && "ALUMNO".equals(roleName)) {
            throw new IllegalArgumentException("Los alumnos deben registrarse con correo @alumno.ipn.mx");
        }
        return roleName;
    }

    private boolean isAlumnoEmail(String email) {
        return email.endsWith("@alumno.ipn.mx");
    }

    private boolean isAutoApprovedRole(String roleName) {
        return "ALUMNO".equals(roleName);
    }

    private boolean isLocalUser(User user) {
        return "LOCAL".equalsIgnoreCase(user.getAuthProvider());
    }

    private boolean hasRole(User user, String roleName) {
        return user.getRoles() != null && user.getRoles().stream()
                .anyMatch(role -> roleName.equals(role.getName()));
    }

    private void invalidatePendingVerificationTokens(User user) {
        var tokens = emailVerifs.findByUserIdAndUsedFalse(user.getId());
        tokens.forEach(token -> token.setUsed(true));
        emailVerifs.saveAll(tokens);
    }

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
