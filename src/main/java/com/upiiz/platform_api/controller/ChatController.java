package com.upiiz.platform_api.controller;

import com.upiiz.platform_api.dto.*;
import com.upiiz.platform_api.security.CurrentUser;
import com.upiiz.platform_api.services.ChatReportService;
import com.upiiz.platform_api.services.ChatService;
import com.upiiz.platform_api.services.RoleSnapshotResolver;
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
    public ConversationResponse createOrGetDirect(@RequestBody CreateDirectConversationRequest req) {
        UUID me = CurrentUser.id();
        return chatService.createOrGetDirect(me, req.userId);
    }

    @GetMapping("/conversations")
    public List<ConversationResponse> myConversations() {
        return chatService.listMyConversations(CurrentUser.id());
    }

    @PostMapping("/conversations/{conversationId}/messages")
    public MessageResponse sendText(@PathVariable Long conversationId, @RequestBody SendMessageRequest req) {
        return chatService.sendText(CurrentUser.id(), conversationId, req);
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public List<MessageResponse> getMessages(
            @PathVariable Long conversationId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant before,
            @RequestParam(defaultValue = "30") int limit
    ) {
        return chatService.getMessages(CurrentUser.id(), conversationId, before, limit);
    }

    @PostMapping(value = "/conversations/{conversationId}/messages/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public MessageResponse sendWithAttachments(
            @PathVariable Long conversationId,
            @RequestPart(required = false) String content,
            @RequestPart(required = false) String clientMessageId,
            @RequestPart("files") List<MultipartFile> files
    ) throws Exception {
        return chatService.sendWithAttachments(CurrentUser.id(), conversationId, content, clientMessageId, files);
    }

    @PostMapping("/reports")
    public ReportSummaryResponse report(@RequestBody CreateReportRequest req) {
        return reportService.createReport(CurrentUser.id(), req, roleResolver);
    }
    @PostMapping("/messages/{messageId}/report")
    public ReportSummaryResponse reportMessage(
            @PathVariable Long messageId,
            @RequestBody CreateReportRequest req
    ) {
        if (req == null) {
            req = new CreateReportRequest();
        }

        req.messageId = messageId;

        return reportService.createReport(CurrentUser.id(), req, roleResolver);
    }
    @GetMapping("/users/search")
    public List<UserSearchResponse> searchUsers(@RequestParam String q) {
        return chatService.searchUsers(q);
    }

    @GetMapping("/attachments/{attachmentId}/download")
    public ResponseEntity<Resource> downloadAttachment(
            @PathVariable Long attachmentId
    ) throws Exception {
        return chatService.downloadAttachment(CurrentUser.id(), attachmentId);
    }
}