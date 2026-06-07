package com.upiiz.platform_api.controller;

import com.upiiz.platform_api.dto.ApiResponse;
import com.upiiz.platform_api.repositories.AnalyticsModerationSummaryProjection;
import com.upiiz.platform_api.repositories.AdminTopicDifficultyProjection;
import com.upiiz.platform_api.repositories.AdminTopicInterestProjection;
import com.upiiz.platform_api.repositories.TeacherImprovementAreaProjection;
import com.upiiz.platform_api.repositories.TeacherPerformanceProjection;
import com.upiiz.platform_api.services.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/upiiz/admin/v1/analytics")
@Tag(name = "Admin - analitica", description = "Indicadores administrativos de intereses, dificultad y desempeno docente")
@SecurityRequirement(name = "bearer-jwt")
public class AnalyticsAdminController {

    private final AnalyticsService analyticsService;

    public AnalyticsAdminController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/general/interest")
    @Operation(summary = "Interes por tema", description = "Obtiene la analitica agregada de interes por categoria y subarea.")
    public ResponseEntity<ApiResponse<List<AdminTopicInterestProjection>>> getGeneralInterest() {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Analítica general de interés obtenida correctamente",
                        analyticsService.getAdminTopicInterest()
                )
        );
    }

    @GetMapping("/general/difficulty")
    @Operation(summary = "Dificultad por tema", description = "Obtiene la analitica agregada de dificultad por categoria y subarea.")
    public ResponseEntity<ApiResponse<List<AdminTopicDifficultyProjection>>> getGeneralDifficulty() {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Analítica general de dificultad obtenida correctamente",
                        analyticsService.getAdminTopicDifficulty()
                )
        );
    }

    @GetMapping({"/moderation/summary", "/moderation/content-excluded"})
    @Operation(summary = "Moderacion y contenido excluido", description = "Devuelve conteos separados de usuarios baneados/activos y reportes resueltos o desestimados.")
    public ResponseEntity<ApiResponse<AnalyticsModerationSummaryProjection>> getModerationSummary() {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Resumen de moderacion obtenido correctamente",
                        analyticsService.getModerationSummary()
                )
        );
    }

    @GetMapping("/teachers/performance")
    @Operation(summary = "Desempeno docente", description = "Lista indicadores de evaluaciones, foros, citas y videoconferencias por docente.")
    public ResponseEntity<ApiResponse<List<TeacherPerformanceProjection>>> getTeacherPerformance() {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Desempeño docente obtenido correctamente",
                        analyticsService.getTeacherPerformance()
                )
        );
    }

    @GetMapping("/teachers/{teacherId}/performance")
    @Operation(summary = "Desempeno de un docente", description = "Obtiene los indicadores de un docente especifico.")
    public ResponseEntity<ApiResponse<TeacherPerformanceProjection>> getTeacherPerformanceByTeacher(
            @PathVariable UUID teacherId
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Desempeño docente obtenido correctamente",
                        analyticsService.getTeacherPerformanceByTeacherId(teacherId)
                )
        );
    }

    @GetMapping("/teachers/improvement-areas")
    @Operation(summary = "Areas de mejora docentes", description = "Lista areas de mejora detectadas a partir de eventos de dificultad.")
    public ResponseEntity<ApiResponse<List<TeacherImprovementAreaProjection>>> getTeacherImprovementAreas() {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Áreas de mejora obtenidas correctamente",
                        analyticsService.getTeacherImprovementAreas()
                )
        );
    }

    @GetMapping("/teachers/{teacherId}/improvement-areas")
    @Operation(summary = "Areas de mejora de un docente", description = "Lista areas de mejora de un docente especifico.")
    public ResponseEntity<ApiResponse<List<TeacherImprovementAreaProjection>>> getTeacherImprovementAreasByTeacher(
            @PathVariable UUID teacherId
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Áreas de mejora del docente obtenidas correctamente",
                        analyticsService.getTeacherImprovementAreasByTeacherId(teacherId)
                )
        );
    }
}
