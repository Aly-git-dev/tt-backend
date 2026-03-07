package com.upiiz.platform_api.services;

import com.upiiz.platform_api.dto.*;
import com.upiiz.platform_api.entities.*;
import com.upiiz.platform_api.repositories.*;
import com.upiiz.platform_api.storage.ChatFileStorage;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private final ChatConversationRepo convRepo;
    private final ChatMessageRepo msgRepo;
    private final ChatAttachmentRepo attRepo;
    private final ChatAccessService access;
    private final ChatModerationService moderation;
    private final ChatFileStorage storage;

    public ChatService(ChatConversationRepo convRepo, ChatMessageRepo msgRepo, ChatAttachmentRepo attRepo,
                       ChatAccessService access, ChatModerationService moderation, ChatFileStorage storage, com.upiiz.platform_api.repositories.UserRoleNativeRepo userRoleRepo) {
        this.convRepo = convRepo;
        this.msgRepo = msgRepo;
        this.attRepo = attRepo;
        this.access = access;
        this.moderation = moderation;
        this.storage = storage;
        this.userRoleRepo = userRoleRepo;
    }

    @org.springframework.transaction.annotation.Transactional
    public com.upiiz.platform_api.dto.ConversationResponse createOrGetDirect(UUID me, UUID other) {
        if (me.equals(other)) throw new IllegalArgumentException("Same user");

        // si el usuario tiene BAN/TEMP_BLOCK en chat, que no pueda iniciar ni mandar (si ya tienes moderation)
        moderation.assertUserCanChat(me);

        // ✅ calcula allowed_pair REAL con roles
        String allowedPair = computeAllowedPairOrThrow(me, other);

        var existing = convRepo.findDirectByUsers(me, other);
        var c = existing.orElseGet(() -> {
            var nc = new com.upiiz.platform_api.entities.ChatConversation();

            // guardamos en orden estable por string (no obligatorio, pero ayuda a consistencia)
            if (me.toString().compareTo(other.toString()) <= 0) {
                nc.setUser1Id(me);
                nc.setUser2Id(other);
            } else {
                nc.setUser1Id(other);
                nc.setUser2Id(me);
            }

            nc.setAllowedPair(allowedPair);
            nc.setLastMessageAt(null);
            return convRepo.save(nc);
        });

        var res = new com.upiiz.platform_api.dto.ConversationResponse();
        res.id = c.getId();
        res.otherUserId = access.otherUser(c, me);
        res.allowedPair = c.getAllowedPair();
        res.lastMessageAt = c.getLastMessageAt();
        return res;
    }

    public List<ConversationResponse> listMyConversations(UUID me) {
        return convRepo.listForUser(me).stream().map(c -> {
            ConversationResponse r = new ConversationResponse();
            r.id = c.getId();
            r.otherUserId = access.otherUser(c, me);
            r.allowedPair = c.getAllowedPair();
            r.lastMessageAt = c.getLastMessageAt();
            return r;
        }).toList();
    }

    @Transactional
    public MessageResponse sendText(UUID me, Long conversationId, SendMessageRequest req) {
        ChatConversation c = access.requireConversation(conversationId);
        access.requireParticipant(c, me);
        moderation.assertUserCanChat(me);

        String content = req.content == null ? null : req.content.trim();
        if (content == null || content.isEmpty()) {
            throw new IllegalArgumentException("Content required (use attachments endpoint for files)");
        }

        // Idempotencia
        if (req.clientMessageId != null && !req.clientMessageId.isBlank()) {
            var existing = msgRepo.findByConversationIdAndSenderIdAndClientMessageId(conversationId, me, req.clientMessageId);
            if (existing.isPresent()) return toMessageResponse(existing.get(), List.of());
        }

        ChatMessage m = new ChatMessage();
        m.setConversationId(conversationId);
        m.setSenderId(me);
        m.setContent(content);
        m.setContentType("TEXT");
        m.setStatus("SENT");
        m.setClientMessageId(req.clientMessageId);
        m = msgRepo.save(m);

        c.setLastMessageAt(Instant.now());
        convRepo.save(c);

        return toMessageResponse(m, List.of());
    }

    @Transactional
    public MessageResponse sendWithAttachments(UUID me, Long conversationId, String content, String clientMessageId, List<MultipartFile> files) throws Exception {
        ChatConversation c = access.requireConversation(conversationId);
        access.requireParticipant(c, me);
        moderation.assertUserCanChat(me);

        String trimmed = content == null ? null : content.trim();
        boolean hasText = trimmed != null && !trimmed.isEmpty();
        boolean hasFiles = files != null && files.stream().anyMatch(f -> f != null && !f.isEmpty());

        if (!hasText && !hasFiles) throw new IllegalArgumentException("Either content or files required");

        // Idempotencia (si el front manda client_message_id también aquí)
        if (clientMessageId != null && !clientMessageId.isBlank()) {
            var existing = msgRepo.findByConversationIdAndSenderIdAndClientMessageId(conversationId, me, clientMessageId);
            if (existing.isPresent()) {
                List<ChatAttachment> atts = attRepo.findByMessageId(existing.get().getId());
                return toMessageResponse(existing.get(), atts);
            }
        }

        ChatMessage m = new ChatMessage();
        m.setConversationId(conversationId);
        m.setSenderId(me);
        m.setContent(hasText ? trimmed : null);
        m.setContentType(hasFiles && hasText ? "MIXED" : (hasFiles ? "FILE" : "TEXT"));
        m.setStatus("SENT");
        m.setClientMessageId(clientMessageId);
        m = msgRepo.save(m);

        List<ChatAttachment> saved = new ArrayList<>();
        if (hasFiles) {
            for (MultipartFile f : files) {
                if (f == null || f.isEmpty()) continue;
                var sf = storage.store(conversationId, m.getId(), f);
                ChatAttachment a = new ChatAttachment();
                a.setMessageId(m.getId());
                a.setOriginalName(sf.originalName());
                a.setMimeType(sf.mimeType() == null ? "application/octet-stream" : sf.mimeType());
                a.setSizeBytes(sf.sizeBytes());
                a.setStoragePath(sf.path());
                saved.add(attRepo.save(a));
            }
        }

        c.setLastMessageAt(Instant.now());
        convRepo.save(c);

        return toMessageResponse(m, saved);
    }

    public List<MessageResponse> getMessages(UUID me, Long conversationId, Instant before, int limit) {
        ChatConversation c = access.requireConversation(conversationId);
        access.requireParticipant(c, me);

        int safeLimit = Math.min(Math.max(limit, 1), 50);
        var pageable = PageRequest.of(0, safeLimit);

        List<ChatMessage> msgs = (before == null)
                ? msgRepo.pageLatest(conversationId, pageable)
                : msgRepo.pageBefore(conversationId, before, pageable);

        // attachments batch
        List<Long> ids = msgs.stream().map(ChatMessage::getId).toList();
        Map<Long, List<ChatAttachment>> byMsg = attRepo.findByMessageIdIn(ids).stream()
                .collect(Collectors.groupingBy(ChatAttachment::getMessageId));

        // devolver en orden cronológico asc para UI (opcional)
        Collections.reverse(msgs);

        return msgs.stream()
                .map(m -> toMessageResponse(m, byMsg.getOrDefault(m.getId(), List.of())))
                .toList();
    }

    private MessageResponse toMessageResponse(ChatMessage m, List<ChatAttachment> atts) {
        MessageResponse r = new MessageResponse();
        r.id = m.getId();
        r.conversationId = m.getConversationId();
        r.senderId = m.getSenderId();
        r.content = m.getContent();
        r.contentType = m.getContentType();
        r.status = m.getStatus();
        r.createdAt = m.getCreatedAt();

        r.attachments = atts.stream().map(a -> {
            AttachmentResponse ar = new AttachmentResponse();
            ar.id = a.getId();
            ar.originalName = a.getOriginalName();
            ar.mimeType = a.getMimeType();
            ar.sizeBytes = a.getSizeBytes();
            return ar;
        }).toList();

        return r;
    }
    private final com.upiiz.platform_api.repositories.UserRoleNativeRepo userRoleRepo;

    private Set<String> currentRolesFromAuth() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return Set.of();
        return auth.getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", "").trim().toUpperCase())
                .collect(java.util.stream.Collectors.toSet());
    }

    private Set<String> rolesOfUser(UUID userId) {
        var roles = userRoleRepo.roleNames(userId);
        if (roles == null) return Set.of();
        return roles.stream()
                .filter(java.util.Objects::nonNull)
                .map(r -> r.trim().toUpperCase())
                .collect(java.util.stream.Collectors.toSet());
    }
    /**
     * Calcula allowed_pair y valida que la pareja sea permitida:
     * PROFESOR-ALUMNO | PROFESOR-ASESOR | ALUMNO-ASESOR
     */
    private String computeAllowedPairOrThrow(java.util.UUID me, java.util.UUID other) {
        var meRoles = currentRolesFromAuth();
        var otherRoles = rolesOfUser(other);

        boolean meProfesor = meRoles.contains("PROFESOR");
        boolean meAlumno   = meRoles.contains("ALUMNO");
        boolean meAsesor   = meRoles.contains("ASESOR");

        boolean otProfesor = otherRoles.contains("PROFESOR");
        boolean otAlumno   = otherRoles.contains("ALUMNO");
        boolean otAsesor   = otherRoles.contains("ASESOR");

        if ((meProfesor && otAlumno) || (meAlumno && otProfesor)) return "PROFESOR-ALUMNO";
        if ((meProfesor && otAsesor) || (meAsesor && otProfesor)) return "PROFESOR-ASESOR";
        if ((meAlumno && otAsesor)   || (meAsesor && otAlumno))   return "ALUMNO-ASESOR";

        throw new org.springframework.security.access.AccessDeniedException("Pair not allowed");
    }
}
