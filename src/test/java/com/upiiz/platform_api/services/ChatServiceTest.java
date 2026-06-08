package com.upiiz.platform_api.services;

import com.upiiz.platform_api.repositories.UserRoleNativeRepo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private UserRoleNativeRepo userRoleRepo;

    private ChatService service;

    @BeforeEach
    void setUp() {
        service = new ChatService(
                null,
                null,
                null,
                null,
                null,
                null,
                userRoleRepo,
                null,
                null
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void adminWithAsesorRoleUsesDatabaseRolesForChatPair() {
        UUID admin = UUID.randomUUID();
        UUID alumno = UUID.randomUUID();
        setAuthenticatedRoles("ADMIN");

        when(userRoleRepo.roleNames(admin)).thenReturn(List.of("ADMIN", "ASESOR"));
        when(userRoleRepo.roleNames(alumno)).thenReturn(List.of("ALUMNO"));

        String pair = service.computeAllowedPairOrThrow(admin, alumno);

        assertEquals("ALUMNO-ASESOR", pair);
    }

    @Test
    void adminWithoutAcademicRoleCannotStartChat() {
        UUID admin = UUID.randomUUID();
        UUID alumno = UUID.randomUUID();

        when(userRoleRepo.roleNames(admin)).thenReturn(List.of("ADMIN"));
        when(userRoleRepo.roleNames(alumno)).thenReturn(List.of("ALUMNO"));

        assertThrows(
                AccessDeniedException.class,
                () -> service.computeAllowedPairOrThrow(admin, alumno)
        );
    }

    @Test
    void fallsBackToAuthenticatedRolesWhenCurrentUserRolesAreMissingInDatabase() {
        UUID asesor = UUID.randomUUID();
        UUID profesor = UUID.randomUUID();
        setAuthenticatedRoles("ASESOR");

        when(userRoleRepo.roleNames(asesor)).thenReturn(List.of());
        when(userRoleRepo.roleNames(profesor)).thenReturn(List.of("PROFESOR"));

        String pair = service.computeAllowedPairOrThrow(asesor, profesor);

        assertEquals("PROFESOR-ASESOR", pair);
    }

    private void setAuthenticatedRoles(String... roles) {
        List<SimpleGrantedAuthority> authorities = List.of(roles).stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user@ipn.mx", null, authorities)
        );
    }
}
