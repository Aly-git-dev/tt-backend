package com.upiiz.platform_api.controller;

import com.upiiz.platform_api.dto.ApiResponse;
import com.upiiz.platform_api.dto.CreateTeacherEvaluationRequest;
import com.upiiz.platform_api.dto.CreateTopicDifficultyEventRequest;
import com.upiiz.platform_api.dto.CreateTopicInterestEventRequest;
import com.upiiz.platform_api.dto.UserSearchResponse;
import com.upiiz.platform_api.entities.TeacherEvaluation;
import com.upiiz.platform_api.entities.TopicDifficultyEvent;
import com.upiiz.platform_api.entities.TopicInterestEvent;
import com.upiiz.platform_api.services.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/upiiz/private/v1/analytics")
@Tag(name = "Analitica - eventos", description = "Registro de evaluaciones docentes y eventos de interes o dificultad")
@SecurityRequirement(name = "bearer-jwt")
public class AnalyticsInteractionController {

    private final AnalyticsService analyticsService;

    public AnalyticsInteractionController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @PostMapping("/teacher-evaluations")
    @Operation(summary = "Registrar evaluacion docente", description = "Guarda una evaluacion de un profesor asociada opcionalmente a una cita.")
    public ResponseEntity<ApiResponse<TeacherEvaluation>> createTeacherEvaluation(
            @RequestBody CreateTeacherEvaluationRequest request
    ) {
        TeacherEvaluation saved = analyticsService.createTeacherEvaluation(request);
        return ResponseEntity.ok(
                ApiResponse.success("Evaluación docente registrada correctamente", saved)
        );
    }

    @GetMapping({"/teachers/search", "/teacher-evaluations/teachers/search"})
    @Operation(summary = "Buscar docentes", description = "Busca docentes activos para registrar evaluaciones.")
    public ResponseEntity<ApiResponse<List<UserSearchResponse>>> searchTeachers(
            @RequestParam(required = false, defaultValue = "") String q
    ) {
        return ResponseEntity.ok(
                ApiResponse.success("Docentes encontrados correctamente", analyticsService.searchTeachers(q))
        );
    }

    @PostMapping("/topic-interest-events")
    @Operation(summary = "Registrar interes de tema", description = "Guarda un evento de interes usado por recomendaciones y analitica.")
    public ResponseEntity<ApiResponse<TopicInterestEvent>> createTopicInterestEvent(
            @RequestBody CreateTopicInterestEventRequest request
    ) {
        TopicInterestEvent saved = analyticsService.createTopicInterestEvent(request);
        return ResponseEntity.ok(
                ApiResponse.success("Evento de interés registrado correctamente", saved)
        );
    }

    @PostMapping("/topic-difficulty-events")
    @Operation(summary = "Registrar dificultad de tema", description = "Guarda un evento de dificultad para analitica academica.")
    public ResponseEntity<ApiResponse<TopicDifficultyEvent>> createTopicDifficultyEvent(
            @RequestBody CreateTopicDifficultyEventRequest request
    ) {
        TopicDifficultyEvent saved = analyticsService.createTopicDifficultyEvent(request);
        return ResponseEntity.ok(
                ApiResponse.success("Evento de dificultad registrado correctamente", saved)
        );
    }
}
