package com.upiiz.platform_api.controller;

import com.upiiz.platform_api.dto.AdminReportDto;
import com.upiiz.platform_api.repositories.UserRepository;
import com.upiiz.platform_api.services.ForumService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ForumAdminControllerTest {

    @Test
    void resolveReportAcceptsMissingBodyAndReturnsUpdatedThreadReport() throws Exception {
        ForumService forumService = mock(ForumService.class);
        UserRepository userRepository = mock(UserRepository.class);
        ForumAdminController controller = new ForumAdminController(forumService, userRepository);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        AdminReportDto resolved = AdminReportDto.builder()
                .id(20L)
                .threadId(10L)
                .threadTitle("Hilo reportado")
                .status("RESUELTO")
                .handledByName("Admin")
                .build();

        when(forumService.resolveReport(eq(20L), isNull(), eq("admin@ipn.mx"))).thenReturn(resolved);

        mockMvc.perform(post("/upiiz/admin/v1/forums/reports/20/resolve")
                        .principal(() -> "admin@ipn.mx"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(20))
                .andExpect(jsonPath("$.threadId").value(10))
                .andExpect(jsonPath("$.status").value("RESUELTO"));

        verify(forumService).resolveReport(eq(20L), isNull(), eq("admin@ipn.mx"));
    }
}
