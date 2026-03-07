package com.upiiz.platform_api.controller;

import com.upiiz.platform_api.dto.AdminHandleReportRequest;
import com.upiiz.platform_api.dto.ReportDetailResponse;
import com.upiiz.platform_api.dto.ReportSummaryResponse;
import com.upiiz.platform_api.entities.ChatMessageReport;
import com.upiiz.platform_api.entities.ChatReportContext;
import com.upiiz.platform_api.repositories.ChatMessageReportRepo;
import com.upiiz.platform_api.repositories.ChatReportContextRepo;
import com.upiiz.platform_api.security.CurrentUser;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/upiiz/admin/v1/admin")
public class ChatAdminController {

    private final ChatMessageReportRepo reportRepo;
    private final ChatReportContextRepo ctxRepo;

    public ChatAdminController(ChatMessageReportRepo reportRepo, ChatReportContextRepo ctxRepo) {
        this.reportRepo = reportRepo;
        this.ctxRepo = ctxRepo;
    }

    private void requireAdmin() {
        if (!CurrentUser.hasRole("ADMIN")) throw new SecurityException("ADMIN required");
    }

    @GetMapping("/reports")
    public List<ReportSummaryResponse> reports(@RequestParam(defaultValue = "PENDIENTE") String status) {
        requireAdmin();
        return reportRepo.findByStatusOrderByCreatedAtDesc(status).stream().map(this::toSummary).toList();
    }

    @GetMapping("/reports/{id}")
    public ReportDetailResponse reportDetail(@PathVariable Long id) {
        requireAdmin();
        ChatMessageReport r = reportRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Report not found"));
        List<ChatReportContext> ctx = ctxRepo.findByReportIdOrderByContextIndexAsc(id);

        ReportDetailResponse out = new ReportDetailResponse();
        out.id = r.getId();
        out.status = r.getStatus();
        out.reasonCode = r.getReasonCode();
        out.createdAt = r.getCreatedAt();
        out.conversationId = r.getConversationId();
        out.reportedMessageId = r.getReportedMessageId();
        out.reporterId = r.getReporterId();
        out.handledBy = r.getHandledBy();
        out.handledAt = r.getHandledAt();

        out.context = ctx.stream().map(c -> {
            var item = new com.upiiz.platform_api.dto.ReportContextItem();
            item.index = c.getContextIndex();
            item.messageId = c.getMessageId();
            item.senderIdSnapshot = c.getSenderIdSnapshot();
            item.senderRoleSnapshot = c.getSenderRoleSnapshot();
            item.contentTypeSnapshot = c.getContentTypeSnapshot();
            item.contentSnapshot = c.getContentSnapshot();
            item.createdAtSnapshot = c.getCreatedAtSnapshot();
            return item;
        }).toList();

        return out;
    }

    @PostMapping("/reports/{id}/resolve")
    public ReportSummaryResponse resolve(@PathVariable Long id, @RequestBody(required = false) AdminHandleReportRequest req) {
        requireAdmin();
        ChatMessageReport r = reportRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Report not found"));
        r.setStatus("RESUELTO");
        r.setHandledBy(CurrentUser.id());
        r.setHandledAt(Instant.now());
        reportRepo.save(r);
        return toSummary(r);
    }

    @PostMapping("/reports/{id}/dismiss")
    public ReportSummaryResponse dismiss(@PathVariable Long id, @RequestBody(required = false) AdminHandleReportRequest req) {
        requireAdmin();
        ChatMessageReport r = reportRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Report not found"));
        r.setStatus("DESESTIMADO");
        r.setHandledBy(CurrentUser.id());
        r.setHandledAt(Instant.now());
        reportRepo.save(r);
        return toSummary(r);
    }

    private ReportSummaryResponse toSummary(ChatMessageReport r) {
        ReportSummaryResponse out = new ReportSummaryResponse();
        out.id = r.getId();
        out.status = r.getStatus();
        out.reasonCode = r.getReasonCode();
        out.createdAt = r.getCreatedAt();
        out.conversationId = r.getConversationId();
        out.reportedMessageId = r.getReportedMessageId();
        return out;
    }
}

