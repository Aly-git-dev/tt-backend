package com.upiiz.platform_api.controller;

import com.upiiz.platform_api.dto.*;
import com.upiiz.platform_api.security.CurrentUser;
import com.upiiz.platform_api.services.ChatReportService;
import com.upiiz.platform_api.services.ChatService;
import com.upiiz.platform_api.services.RoleSnapshotResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/upiiz/admin/v1/chats")
@Tag(name = "Chat", description = "Conversaciones directas, mensajes, adjuntos y reportes de chat")
@SecurityRequirement(name = "bearer-jwt")
public class ChatController {

    private final ChatService chatService;
    private final ChatReportService reportService;
    private final RoleSnapshotResolver roleResolver;

    public ChatController(ChatService chatService, ChatReportService reportService, RoleSnapshotResolver roleResolver) {
        this.chatService = chatService;
        this.reportService = reportService;
        this.roleResolver = roleResolver;
    }

    @PostMapping("/conversations/direct")
    @Operation(summary = "Crear u obtener conversacion directa", description = "Crea una conversacion directa permitida o devuelve la existente.")
    public ConversationResponse createOrGetDirect(@RequestBody CreateDirectConversationRequest req) {
        UUID me = CurrentUser.id();
        return chatService.createOrGetDirect(me, req.userId);
    }

    @GetMapping("/conversations")
    @Operation(summary = "Listar conversaciones", description = "Devuelve las conversaciones del usuario autenticado.")
    public List<ConversationResponse> myConversations() {
        return chatService.listMyConversations(CurrentUser.id());
    }

    @PostMapping("/conversations/{conversationId}/messages")
    @Operation(summary = "Enviar mensaje de texto", description = "Envia un mensaje de texto dentro de una conversacion.")
    public MessageResponse sendText(@PathVariable Long conversationId, @RequestBody SendMessageRequest req) {
        return chatService.sendText(CurrentUser.id(), conversationId, req);
    }

    @GetMapping("/conversations/{conversationId}/messages")
    @Operation(summary = "Listar mensajes", description = "Devuelve mensajes paginados de una conversacion y marca como leidos los recibidos.")
    public List<MessageResponse> getMessages(
            @PathVariable Long conversationId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant before,
            @RequestParam(defaultValue = "30") int limit
    ) {
        return chatService.getMessages(CurrentUser.id(), conversationId, before, limit);
    }

    @PostMapping(value = "/conversations/{conversationId}/messages/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Enviar mensaje con adjuntos", description = "Envia texto, archivos o ambos dentro de una conversacion.")
    public MessageResponse sendWithAttachments(
            @PathVariable Long conversationId,
            @RequestPart(required = false) String content,
            @RequestPart(required = false) String clientMessageId,
            @RequestPart("files") List<MultipartFile> files
    ) throws Exception {
        return chatService.sendWithAttachments(CurrentUser.id(), conversationId, content, clientMessageId, files);
    }

    @PostMapping("/reports")
    @Operation(summary = "Reportar mensaje", description = "Crea un reporte de chat con contexto para revision administrativa.")
    public ReportSummaryResponse report(@RequestBody CreateReportRequest req) {
        return reportService.createReport(CurrentUser.id(), req, roleResolver);
    }
    @PostMapping("/messages/{messageId}/report")
    @Operation(summary = "Reportar mensaje por ID", description = "Crea un reporte tomando el mensaje reportado desde la ruta.")
    public ReportSummaryResponse reportMessage(
            @PathVariable Long messageId,
            @RequestBody(required = false) CreateReportRequest req
    ) {
        if (req == null) {
            req = new CreateReportRequest();
        }

        req.messageId = messageId;

        return reportService.createReport(CurrentUser.id(), req, roleResolver);
    }
    @GetMapping("/users/search")
    @Operation(summary = "Buscar usuarios para chat", description = "Busca usuarios por correo institucional para iniciar conversaciones.")
    public List<UserSearchResponse> searchUsers(@RequestParam String q) {
        return chatService.searchUsers(q);
    }

    @GetMapping("/attachments/{attachmentId}/download")
    @Operation(summary = "Descargar adjunto de chat", description = "Descarga un archivo adjunto si el usuario pertenece a la conversacion.")
    public ResponseEntity<Resource> downloadAttachment(
            @PathVariable Long attachmentId
    ) throws Exception {
        return chatService.downloadAttachment(CurrentUser.id(), attachmentId);
    }
}
