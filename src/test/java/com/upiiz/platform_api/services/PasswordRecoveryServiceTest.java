package com.upiiz.platform_api.services;

import com.upiiz.platform_api.dto.ResetPasswordRequest;
import com.upiiz.platform_api.entities.PasswordResetToken;
import com.upiiz.platform_api.entities.Role;
import com.upiiz.platform_api.entities.User;
import com.upiiz.platform_api.repositories.PasswordResetTokenRepository;
import com.upiiz.platform_api.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordRecoveryServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private MailService mailService;

    @InjectMocks
    private PasswordRecoveryService service;

    @Test
    void requestPasswordResetAllowsAlumnoLocalAccountAndSendsEmail() {
        User user = localUser("ALUMNO");
        PasswordResetToken previousToken = PasswordResetToken.builder()
                .token("previous")
                .user(user)
                .used(false)
                .createdAt(LocalDateTime.now().minusMinutes(10))
                .expiresAt(LocalDateTime.now().plusMinutes(20))
                .build();

        when(userRepository.findByEmailInstIgnoreCase("user@alumno.ipn.mx")).thenReturn(Optional.of(user));
        when(tokenRepository.findByUserAndUsedFalse(user)).thenReturn(List.of(previousToken));

        service.requestPasswordReset(" USER@ALUMNO.IPN.MX ");

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).saveAll(List.of(previousToken));
        verify(tokenRepository).save(tokenCaptor.capture());

        PasswordResetToken savedToken = tokenCaptor.getValue();
        assertTrue(previousToken.isUsed());
        assertFalse(savedToken.isUsed());
        assertDoesNotThrow(() -> UUID.fromString(savedToken.getToken()));
        assertEquals(user, savedToken.getUser());
        assertTrue(savedToken.getExpiresAt().isAfter(savedToken.getCreatedAt()));

        verify(mailService).sendPasswordResetEmail("user@alumno.ipn.mx", savedToken.getToken());
    }

    @Test
    void requestPasswordResetDoesNotSendForGescoAccount() {
        User user = localUser("ALUMNO");
        user.setAuthProvider("GESCO");

        when(userRepository.findByEmailInstIgnoreCase("user@alumno.ipn.mx")).thenReturn(Optional.of(user));

        service.requestPasswordReset("user@alumno.ipn.mx");

        verify(tokenRepository, never()).findByUserAndUsedFalse(any());
        verify(tokenRepository, never()).save(any());
        verify(mailService, never()).sendPasswordResetEmail(any(), any());
    }

    @Test
    void resetPasswordUpdatesHashAndMarksTokenAsUsed() {
        User user = localUser("PROFESOR");
        PasswordResetToken token = PasswordResetToken.builder()
                .token("reset-token")
                .user(user)
                .used(false)
                .createdAt(LocalDateTime.now().minusMinutes(5))
                .expiresAt(LocalDateTime.now().plusMinutes(25))
                .build();

        when(tokenRepository.findByToken("reset-token")).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("newSecret123")).thenReturn("new-hash");

        service.resetPassword(new ResetPasswordRequest(" reset-token ", "newSecret123", "newSecret123"));

        assertEquals("new-hash", user.getPasswordHash());
        assertTrue(token.isUsed());
        verify(userRepository).save(user);
        verify(tokenRepository).save(token);
    }

    private User localUser(String roleName) {
        Role role = Role.builder()
                .name(roleName)
                .build();

        User user = new User();
        user.setEmailInst("user@alumno.ipn.mx");
        user.setPasswordHash("old-hash");
        user.setActive(true);
        user.setAuthProvider("LOCAL");
        user.setRoles(Set.of(role));
        return user;
    }
}
