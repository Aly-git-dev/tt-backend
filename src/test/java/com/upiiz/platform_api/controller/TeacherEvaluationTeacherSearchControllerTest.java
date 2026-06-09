package com.upiiz.platform_api.controller;

import com.upiiz.platform_api.dto.ApiResponse;
import com.upiiz.platform_api.dto.UserSearchResponse;
import com.upiiz.platform_api.services.UserSearchService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeacherEvaluationTeacherSearchControllerTest {

    @Mock
    private UserSearchService userSearchService;

    @Test
    void searchTeachersReturnsTeacherResultsForEvaluationFlow() {
        TeacherEvaluationTeacherSearchController controller =
                new TeacherEvaluationTeacherSearchController(userSearchService);
        UserSearchResponse teacher = new UserSearchResponse();
        teacher.id = UUID.randomUUID();
        teacher.email = "ada@ipn.mx";
        teacher.name = "Ada Lovelace";
        when(userSearchService.searchTeachers("Ada")).thenReturn(List.of(teacher));

        var response = controller.searchTeachers("Ada");
        ApiResponse<List<UserSearchResponse>> body = response.getBody();

        assertEquals(200, response.getStatusCode().value());
        assertTrue(body.isOk());
        assertEquals("Docentes encontrados correctamente", body.getMessage());
        assertEquals(List.of(teacher), body.getData());
        verify(userSearchService).searchTeachers("Ada");
    }
}
