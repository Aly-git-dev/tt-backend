package com.upiiz.platform_api.services;
import com.upiiz.platform_api.dto.*;
import com.upiiz.platform_api.entities.*;
import com.upiiz.platform_api.repositories.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class ChatReportService {

    private final ChatMessageRepo msgRepo;
    private final ChatConversationRepo convRepo;
    private final ChatMessageReportRepo reportRepo;
    private final ChatReportContextRepo ctxRepo;
    private final ChatAccessService access;

    public ChatReportService(ChatMessageRepo msgRepo, ChatConversationRepo convRepo,
                             ChatMessageReportRepo reportRepo, ChatReportContextRepo ctxRepo,
                             ChatAccessService access) {
        this.msgRepo = msgRepo;
        this.convRepo = convRepo;
        this.reportRepo = reportRepo;
        this.ctxRepo = ctxRepo;
        this.access = access;
    }

    @Transactional
    public ReportSummaryResponse createReport(UUID me, CreateReportRequest req, RoleSnapshotResolver roleResolver) {
        if (req == null || req.messageId == null) throw new IllegalArgumentException("messageId required");
        if (req.reasonCode == null || req.reasonCode.isBlank()) throw new IllegalArgumentException("reasonCode required");

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
        r = reportRepo.save(r);

        // Traer reportado + 5 anteriores (total 6) por createdAt <= reportedAt
        List<ChatMessage> contextDesc = msgRepo.contextForReport(conv.getId(), reported.getCreatedAt(), PageRequest.of(0, 6));

        // Asegurar que el reportado esté incluido, y ponerlo en index 0
        Map<Long, ChatMessage> map = new HashMap<>();
        for (ChatMessage m : contextDesc) map.put(m.getId(), m);

        List<ChatMessage> ordered = new ArrayList<>();
        ordered.add(reported);

        // Los anteriores: tomar de contextDesc (que viene desc), saltar el reportado, hasta 5
        for (ChatMessage m : contextDesc) {
            if (m.getId().equals(reported.getId())) continue;
            ordered.add(m);
            if (ordered.size() == 6) break;
        }

        // Guardar snapshot 0..5
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
        return out;
    }
}

