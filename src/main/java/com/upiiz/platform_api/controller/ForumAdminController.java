package com.upiiz.platform_api.controller;

import com.upiiz.platform_api.dto.AdminReportDto;
import com.upiiz.platform_api.dto.AdminUserSummaryDto;
import com.upiiz.platform_api.dto.ReportAdminActionDto;
import com.upiiz.platform_api.entities.User;
import com.upiiz.platform_api.repositories.UserRepository;
import com.upiiz.platform_api.services.ForumService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/upiiz/admin/v1/forums")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - foros", description = "Moderacion de reportes y usuarios en foros")
@SecurityRequirement(name = "bearer-jwt")
public class ForumAdminController {

    private final ForumService forumService;
    private final UserRepository userRepo;

    // Lista sólo pendientes (para el dashboard admin)
    @GetMapping("/reports")
    @Operation(summary = "Listar reportes pendientes", description = "Devuelve los reportes de foro pendientes de revision.")
    public List<AdminReportDto> getPendingReports() {
        return forumService.getPendingReportsForAdmin();
    }

    // Lista todos los reportes (histórico)
    @GetMapping("/reports/all")
    @Operation(summary = "Listar todos los reportes", description = "Devuelve el historial completo de reportes de foro.")
    public List<AdminReportDto> getAllReports() {
        return forumService.getAllReportsForAdmin();
    }

    // Resolver un reporte
    @PostMapping("/reports/{id}/resolve")
    @Operation(summary = "Resolver reporte de foro", description = "Aplica la accion administrativa configurada y marca el reporte como resuelto.")
    public void resolveReport(
            @PathVariable Long id,
            @RequestBody ReportAdminActionDto dto,
            Principal principal
    ) {
        // asumimos que principal.getName() = email_inst del admin (igual que en el resto del sistema)
        String adminEmail = principal.getName();
        forumService.resolveReport(id, dto, adminEmail);
    }

    @PostMapping("/reports/{id}/dismiss")
    @Operation(summary = "Desestimar reporte de foro", description = "Marca un reporte de foro como desestimado sin ocultar contenido ni banear usuarios.")
    public void dismissReport(
            @PathVariable Long id,
            @RequestBody(required = false) ReportAdminActionDto dto,
            Principal principal
    ) {
        String adminEmail = principal.getName();
        forumService.dismissReport(id, dto, adminEmail);
    }

    // GET /upiiz/api/v1/admin/users/banned
    @GetMapping("/banned")
    @Operation(summary = "Listar usuarios baneados", description = "Lista usuarios desactivados desde moderacion.")
    public List<AdminUserSummaryDto> getBannedUsers() {
        // Aquí asumo que "baneado" = active = false
        List<User> users = userRepo.findByActiveFalseOrderByNombreAsc();

        return users.stream()
                .map(u -> AdminUserSummaryDto.builder()
                        .id(u.getId())
                        .emailInst(u.getEmailInst())
                        .fullName(u.getNombre())   // o getFull_name si así se llama
                        .carrera(u.getCarrera())
                        .active(u.isActive())
                        .build())
                .toList();
    }

    // POST /upiiz/api/v1/admin/users/{id}/unban
    @PostMapping("/{id}/unban")
    @Operation(summary = "Desbanear usuario", description = "Reactiva un usuario desactivado por moderacion.")
    public ResponseEntity<Void> unbanUser(@PathVariable UUID id) {
        User user = userRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        user.setActive(true);
        // opcional limpiar campos de ban:
        // user.setBannedAt(null);
        // user.setBannedBy(null);
        userRepo.save(user);

        return ResponseEntity.noContent().build();
    }
}
