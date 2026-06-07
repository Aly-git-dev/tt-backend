package com.upiiz.platform_api.services;

import com.upiiz.platform_api.auth.AuthStatusException;
import com.upiiz.platform_api.auth.dto.LoginRequest;
import com.upiiz.platform_api.auth.dto.RegisterRequest;
import com.upiiz.platform_api.entities.EmailVerification;
import com.upiiz.platform_api.entities.Role;
import com.upiiz.platform_api.entities.User;
import com.upiiz.platform_api.repositories.EmailVerificationRepository;
import com.upiiz.platform_api.repositories.RoleRepository;
import com.upiiz.platform_api.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authManager;

    @Mock
    private UserRepository users;

    @Mock
    private RoleRepository roles;

    @Mock
    private EmailVerificationRepository emailVerifs;

    @Mock
    private PasswordEncoder encoder;

    @Mock
    private JwtService jwt;

    @Mock
    private MailService mail;

    private AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(
                authManager,
                users,
                roles,
                emailVerifs,
                encoder,
                jwt,
                mail,
                "https://api.example"
        );
    }

    @Test
    void loginReturnsStructuredCodeWhenEmailIsNotVerified() {
        User user = localUser("ALUMNO");
        user.setEmailVerified(false);
        user.setApproved(true);

        when(users.findByEmailInstIgnoreCase("user@alumno.ipn.mx")).thenReturn(Optional.of(user));
        when(authManager.authenticate(any(Authentication.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken(user.getEmailInst(), null));

        AuthStatusException ex = assertThrows(
                AuthStatusException.class,
                () -> service.login(new LoginRequest(" USER@ALUMNO.IPN.MX ", "secret123"))
        );

        assertEquals("EMAIL_NOT_VERIFIED", ex.getCode());
        assertEquals("RESEND_VERIFICATION", ex.getAction());
    }

    @Test
    void loginReturnsStructuredCodeWhenAccountIsInactive() {
        User user = localUser("ALUMNO");
        user.setActive(false);
        user.setEmailVerified(true);
        user.setApproved(true);

        when(users.findByEmailInstIgnoreCase("user@alumno.ipn.mx")).thenReturn(Optional.of(user));
        when(authManager.authenticate(any(Authentication.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken(user.getEmailInst(), null));

        AuthStatusException ex = assertThrows(
                AuthStatusException.class,
                () -> service.login(new LoginRequest("user@alumno.ipn.mx", "secret123"))
        );

        assertEquals("ACCOUNT_INACTIVE", ex.getCode());
        assertEquals("CONTACT_ADMIN", ex.getAction());
    }

    @Test
    void registerBuildsConfirmationLinkWithConfiguredApiUrl() {
        UUID token = UUID.randomUUID();
        Role role = Role.builder().name("ALUMNO").build();
        RegisterRequest request = RegisterRequest.builder()
                .emailInst("USER@ALUMNO.IPN.MX")
                .fullName("User Test")
                .password("secret123")
                .role("ALUMNO")
                .build();

        when(roles.findByName("ALUMNO")).thenReturn(role);
        when(users.findByEmailInstIgnoreCase("user@alumno.ipn.mx")).thenReturn(Optional.empty());
        when(encoder.encode("secret123")).thenReturn("hash");
        when(users.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            if (user.getId() == null) {
                user.setId(UUID.randomUUID());
            }
            return user;
        });
        when(emailVerifs.save(any(EmailVerification.class))).thenAnswer(invocation -> {
            EmailVerification verification = invocation.getArgument(0);
            verification.setToken(token);
            return verification;
        });

        service.register(request);

        verify(mail).sendVerificationEmail(
                "user@alumno.ipn.mx",
                "https://api.example/upiiz/public/v1/auth/confirm?token=" + token
        );
    }

    @Test
    void confirmEmailAcceptsPreviouslyInvalidatedUnexpiredToken() {
        UUID token = UUID.randomUUID();
        User user = localUser("ALUMNO");
        EmailVerification verification = new EmailVerification();
        verification.setToken(token);
        verification.setUserId(user.getId());
        verification.setExpiresAt(Instant.now().plusSeconds(3600));
        verification.setUsed(true);

        when(emailVerifs.findById(token)).thenReturn(Optional.of(verification));
        when(users.findById(user.getId())).thenReturn(Optional.of(user));
        when(emailVerifs.findByUserIdAndUsedFalse(user.getId())).thenReturn(List.of());

        var response = service.confirmEmail(token);

        assertEquals(1, response.get("estado"));
        assertTrue(user.isEmailVerified());
        assertTrue(user.isApproved());
        assertTrue(verification.isUsed());
        verify(users).save(user);
        verify(emailVerifs).save(verification);
    }

    private User localUser(String roleName) {
        Role role = Role.builder()
                .name(roleName)
                .build();

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmailInst("user@alumno.ipn.mx");
        user.setPasswordHash("hash");
        user.setActive(true);
        user.setAuthProvider("LOCAL");
        user.setRoles(Set.of(role));
        return user;
    }
}
