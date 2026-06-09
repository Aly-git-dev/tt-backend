package com.upiiz.platform_api.controller;

import com.upiiz.platform_api.dto.ApiResponse;
import com.upiiz.platform_api.dto.UserSearchResponse;
import com.upiiz.platform_api.services.UserSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/upiiz/private/v1/teacher-evaluations")
@Tag(name = "Evaluaciones docentes", description = "Soporte para crear evaluaciones docentes")
@SecurityRequirement(name = "bearer-jwt")
public class TeacherEvaluationTeacherSearchController {

    private final UserSearchService userSearchService;

    public TeacherEvaluationTeacherSearchController(UserSearchService userSearchService) {
        this.userSearchService = userSearchService;
    }

    @GetMapping("/teachers/search")
    @Operation(summary = "Buscar docentes para evaluar", description = "Busca docentes activos por nombre o correo para seleccionarlos en una evaluacion.")
    public ResponseEntity<ApiResponse<List<UserSearchResponse>>> searchTeachers(
            @RequestParam(required = false, defaultValue = "") String q
    ) {
        return ResponseEntity.ok(
                ApiResponse.success("Docentes encontrados correctamente", userSearchService.searchTeachers(q))
        );
    }
}
