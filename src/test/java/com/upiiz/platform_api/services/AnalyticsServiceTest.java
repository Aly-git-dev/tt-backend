package com.upiiz.platform_api.services;

import com.upiiz.platform_api.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private UserRepository userRepo;

    @Test
    void searchTeachersUsesProfesorRoleAndMapsResponse() {
        UserSearchService userSearchService = new UserSearchService(userRepo);
        AnalyticsService service = new AnalyticsService(null, null, null, null, userSearchService);
        UUID teacherId = UUID.randomUUID();
        when(userRepo.searchActiveUsersByRoles(eq("Ada"), eq(List.of("PROFESOR")), any(Pageable.class)))
                .thenReturn(List.<Object[]>of(new Object[]{teacherId, "ada@ipn.mx", "Ada Lovelace"}));

        var result = service.searchTeachers("  Ada  ");

        assertEquals(1, result.size());
        assertEquals(teacherId, result.get(0).id);
        assertEquals("ada@ipn.mx", result.get(0).email);
        assertEquals("Ada Lovelace", result.get(0).name);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<String>> rolesCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(userRepo).searchActiveUsersByRoles(eq("Ada"), rolesCaptor.capture(), any(Pageable.class));
        assertEquals(List.of("PROFESOR"), List.copyOf(rolesCaptor.getValue()));
    }
}
