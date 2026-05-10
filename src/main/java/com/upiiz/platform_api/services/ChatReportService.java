package com.upiiz.platform_api.services;
import com.upiiz.platform_api.dto.*;
import com.upiiz.platform_api.entities.*;
import com.upiiz.platform_api.repositories.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.*;

@Service
public class ChatReportService {

    private final ChatMessageRepo msgRepo;
    private final ChatConversationRepo convRepo;
    private final ChatMessageReportRepo reportRepo;
    private final ChatReportContextRepo ctxRepo;
    private final ChatAccessService access;
    private final NotificationService notificationService;

    public ChatReportService(ChatMessageRepo msgRepo, ChatConversationRepo convRepo,
                             ChatMessageReportRepo reportRepo, ChatReportContextRepo ctxRepo,
                             ChatAccessService access, NotificationService notificationService) {
        this.msgRepo = msgRepo;
        this.convRepo = convRepo;
        this.reportRepo = reportRepo;
        this.ctxRepo = ctxRepo;
        this.access = access;
        this.notificationService = notificationService;
    }

    @Transactional
    public ReportSummaryResponse createReport(UUID me, CreateReportRequest req, RoleSnapshotResolver roleResolver) {

        if (req == null || req.messageId == null) {
            throw new IllegalArgumentException("messageId required");
        }

        if (req.reasonCode == null || req.reasonCode.isBlank()) {
            req.reasonCode = "OTRO"; // 🔥 evita 500 por vacío
        }

        ChatMessage reported = msgRepo.findById(req.messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found"));

        ChatConversation conv = convRepo.findById(reported.getConversationId())
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

        access.requireParticipant(conv, me);

        ChatMessageReport r = new ChatMessageReport();
        r.setReporterId(me);
        r.setConversationId(conv.getId());
        r.setReportedMessageId(reported.getId());
        r.setReasonCode(req.reasonCode.trim());
        r.setDescription(req.description);
        r.setStatus("PENDIENTE");
        r.setCreatedAt(Instant.now());

        r = reportRepo.save(r);

        // 🔎 CONTEXTO
        List<ChatMessage> contextDesc = msgRepo.contextForReport(
                conv.getId(),
                reported.getCreatedAt(),
                PageRequest.of(0, 6)
        );

        List<ChatMessage> ordered = new ArrayList<>();
        ordered.add(reported);

        for (ChatMessage m : contextDesc) {
            if (m.getId().equals(reported.getId())) continue;
            ordered.add(m);
            if (ordered.size() == 6) break;
        }

        for (int i = 0; i < ordered.size(); i++) {
            ChatMessage m = ordered.get(i);

            ChatReportContext ctx = new ChatReportContext();
            ctx.setReportId(r.getId());
            ctx.setContextIndex((short) i);
            ctx.setMessageId(m.getId());
            ctx.setSenderIdSnapshot(m.getSenderId());
            ctx.setSenderRoleSnapshot(roleResolver.primaryRoleOf(m.getSenderId()));
            ctx.setContentSnapshot(m.getContent());
            ctx.setContentTypeSnapshot(m.getContentType());
            ctx.setCreatedAtSnapshot(m.getCreatedAt());

            ctxRepo.save(ctx);
        }

        ReportSummaryResponse out = new ReportSummaryResponse();
        out.id = r.getId();
        out.status = r.getStatus();
        out.reasonCode = r.getReasonCode();
        out.createdAt = r.getCreatedAt();
        out.conversationId = r.getConversationId();
        out.reportedMessageId = r.getReportedMessageId();

        notifyAfterCommit(me, r.getId());

        return out;
    }

    private void notifyAfterCommit(UUID reporterId, Long reportId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            sendReportNotifications(reporterId, reportId);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                sendReportNotifications(reporterId, reportId);
            }
        });
    }

    private void sendReportNotifications(UUID reporterId, Long reportId) {
        try {
            notificationService.notifyMessageReportSent(reporterId, reportId);
            notificationService.notifyAdminsMessageReported(reportId);
        } catch (Exception e) {
            System.err.println("Error en notificaciones de reporte de mensaje: " + e.getMessage());
        }
    }
}

