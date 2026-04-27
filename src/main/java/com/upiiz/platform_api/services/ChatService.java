package com.upiiz.platform_api.services;

import com.upiiz.platform_api.dto.*;
import com.upiiz.platform_api.entities.ChatAttachment;
import com.upiiz.platform_api.entities.ChatConversation;
import com.upiiz.platform_api.entities.ChatMessage;
import com.upiiz.platform_api.repositories.*;
import com.upiiz.platform_api.storage.ChatFileStorage;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.*;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class ChatService {

    private final ChatConversationRepo convRepo;
    private final ChatMessageRepo msgRepo;
    private final ChatAttachmentRepo attRepo;
    private final ChatAccessService access;
    private final ChatModerationService moderation;
    private final ChatFileStorage storage;
    private final UserRoleNativeRepo userRoleRepo;
    private final UserRepository userRepo;

    public ChatService(
            ChatConversationRepo convRepo,
            ChatMessageRepo msgRepo,
            ChatAttachmentRepo attRepo,
            ChatAccessService access,
            ChatModerationService moderation,
            ChatFileStorage storage,
            UserRoleNativeRepo userRoleRepo,
            UserRepository userRepo
    ) {
        this.convRepo = convRepo;
        this.msgRepo = msgRepo;
        this.attRepo = attRepo;
        this.access = access;
        this.moderation = moderation;
        this.storage = storage;
        this.userRoleRepo = userRoleRepo;
        this.userRepo = userRepo;
    }

    @Transactional
    public ConversationResponse createOrGetDirect(UUID me, UUID other) {
        if (me.equals(other)) {
            throw new IllegalArgumentException("Same user");
        }

        moderation.assertUserCanChat(me);

        String allowedPair = computeAllowedPairOrThrow(me, other);

        ChatConversation c = convRepo.findDirectByUsers(me, other)
                .orElseGet(() -> {
                    ChatConversation nc = new ChatConversation();

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

        return toConversationResponse(c, me);
    }

    @Transactional(readOnly = true)
    public List<ConversationResponse> listMyConversations(UUID me) {
        return convRepo.listForUser(me).stream()
                .map(c -> toConversationResponse(c, me))
                .toList();
    }

    @Transactional
    public MessageResponse sendText(UUID me, Long conversationId, SendMessageRequest req) {
        ChatConversation c = access.requireConversation(conversationId);
        access.requireParticipant(c, me);
        moderation.assertUserCanChat(me);

        String content = req == null || req.content == null ? null : req.content.trim();
        if (content == null || content.isEmpty()) {
            throw new IllegalArgumentException("Content required (use attachments endpoint for files)");
        }

        if (req.clientMessageId != null && !req.clientMessageId.isBlank()) {
            Optional<ChatMessage> existing = msgRepo.findByConversationIdAndSenderIdAndClientMessageId(
                    conversationId, me, req.clientMessageId
            );
            if (existing.isPresent()) {
                return toMessageResponse(existing.get(), List.of());
            }
        }

        ChatMessage m = new ChatMessage();
        m.setConversationId(conversationId);
        m.setSenderId(me);
        m.setContent(content);
        m.setContentType("TEXT");
        m.setStatus("SENT");
        m.setClientMessageId(req.clientMessageId);
        m = msgRepo.save(m);

        c.setLastMessageAt(m.getCreatedAt() != null ? m.getCreatedAt() : Instant.now());
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

        if (!hasText && !hasFiles) {
            throw new IllegalArgumentException("Either content or files required");
        }

        if (clientMessageId != null && !clientMessageId.isBlank()) {
            Optional<ChatMessage> existing = msgRepo.findByConversationIdAndSenderIdAndClientMessageId(
                    conversationId, me, clientMessageId
            );
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

                ChatFileStorage.StoredFile sf = storage.store(conversationId, m.getId(), f);

                ChatAttachment a = new ChatAttachment();
                a.setMessageId(m.getId());
                a.setOriginalName(sf.originalName());
                a.setMimeType(sf.mimeType() == null ? "application/octet-stream" : sf.mimeType());
                a.setSizeBytes(sf.sizeBytes());
                a.setStoragePath(sf.path());
                a.setCreatedAt(Instant.now());
                saved.add(attRepo.save(a));
            }
        }

        c.setLastMessageAt(m.getCreatedAt() != null ? m.getCreatedAt() : Instant.now());
        convRepo.save(c);

        return toMessageResponse(m, saved);
    }

    @Transactional
    public List<MessageResponse> getMessages(UUID me, Long conversationId, Instant before, int limit) {
        ChatConversation c = access.requireConversation(conversationId);
        access.requireParticipant(c, me);

        int safeLimit = Math.min(Math.max(limit, 1), 50);

        msgRepo.markConversationAsRead(conversationId, me);

        List<ChatMessage> msgs = (before == null)
                ? msgRepo.pageLatest(conversationId, PageRequest.of(0, safeLimit))
                : msgRepo.pageBefore(conversationId, before, PageRequest.of(0, safeLimit));

        List<Long> ids = msgs.stream().map(ChatMessage::getId).toList();
        Map<Long, List<ChatAttachment>> byMsg = ids.isEmpty()
                ? Map.of()
                : attRepo.findByMessageIdIn(ids).stream()
                .collect(Collectors.groupingBy(ChatAttachment::getMessageId));

        Collections.reverse(msgs);

        return msgs.stream()
                .map(m -> toMessageResponse(m, byMsg.getOrDefault(m.getId(), List.of())))
                .toList();
    }

    private ConversationResponse toConversationResponse(ChatConversation c, UUID me) {
        UUID otherUserId = access.otherUser(c, me);

        ChatMessage lastMessage = (ChatMessage) msgRepo.findTopByConversationIdOrderByCreatedAtDesc(c.getId()).orElse(null);
        long unreadCount = msgRepo.countUnreadForUser(c.getId(), me);

        ConversationResponse r = new ConversationResponse();
        r.id = c.getId();
        r.otherUserId = otherUserId;
        r.otherName = resolveOtherUserName(otherUserId);
        r.otherAvatarUrl = resolveOtherUserAvatar(otherUserId);
        r.allowedPair = c.getAllowedPair();
        r.lastMessageAt = c.getLastMessageAt();
        r.lastMessagePreview = buildPreview(lastMessage);
        r.lastMessageSenderId = lastMessage != null ? lastMessage.getSenderId() : null;
        r.unreadCount = unreadCount;
        return r;
    }

    private MessageResponse toMessageResponse(ChatMessage m, List<ChatAttachment> atts) {
        MessageResponse r = new MessageResponse();
        r.id = m.getId();
        r.conversationId = m.getConversationId();
        r.senderId = m.getSenderId();
        r.content = m.getContent();
        r.contentType = m.getContentType();
        r.clientMessageId = m.getClientMessageId();
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

    private String buildPreview(ChatMessage m) {
        if (m == null) {
            return "Sin mensajes todavía";
        }

        String type = m.getContentType();
        String content = safe(m.getContent());

        if ("FILE".equals(type)) {
            return "📎 Archivo adjunto";
        }

        if ("MIXED".equals(type)) {
            return content.isBlank() ? "📎 Archivo adjunto" : truncate(content, 60);
        }

        if ("SYSTEM".equals(type)) {
            return "Mensaje del sistema";
        }

        return content.isBlank() ? "" : truncate(content, 60);
    }

    private String truncate(String value, int max) {
        if (value == null) return "";
        return value.length() > max ? value.substring(0, max) + "..." : value;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private Set<String> currentRolesFromAuth() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return Set.of();

        return auth.getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", "").trim().toUpperCase())
                .collect(Collectors.toSet());
    }

    private Set<String> rolesOfUser(UUID userId) {
        var roles = userRoleRepo.roleNames(userId);
        if (roles == null) return Set.of();

        return roles.stream()
                .filter(Objects::nonNull)
                .map(r -> r.trim().toUpperCase())
                .collect(Collectors.toSet());
    }

    private String computeAllowedPairOrThrow(UUID me, UUID other) {
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

        throw new AccessDeniedException("Pair not allowed");
    }

    /*
     * TEMPORAL:
     * Aquí puedes conectar después tu tabla real de usuarios/perfiles.
     * Mientras tanto, el front ya no se rompe.
     */
    private String resolveOtherUserName(UUID otherUserId) {
        return otherUserId != null ? otherUserId.toString() : null;
    }

    private String resolveOtherUserAvatar(UUID otherUserId) {
        return null;
    }

    public List<UserSearchResponse> searchUsers(String query) {
        return userRepo.searchByEmail(query).stream().map(obj -> {
            UserSearchResponse r = new UserSearchResponse();
            r.id = (UUID) obj[0];
            r.email = (String) obj[1];
            r.name = (String) obj[2];
            return r;
        }).toList();
    }

    @Transactional(readOnly = true)
    public ResponseEntity<Resource> downloadAttachment(UUID me, Long attachmentId) throws MalformedURLException {
        ChatAttachment a = attRepo.findById(attachmentId)
                .orElseThrow(() -> new IllegalArgumentException("Archivo no encontrado"));

        ChatMessage m = msgRepo.findById(a.getMessageId())
                .orElseThrow(() -> new IllegalArgumentException("Mensaje no encontrado"));

        ChatConversation c = access.requireConversation(m.getConversationId());
        access.requireParticipant(c, me);

        Path path = Paths.get(a.getStoragePath()).toAbsolutePath().normalize();
        Resource resource = new UrlResource(path.toUri());

        if (!resource.exists() || !resource.isReadable()) {
            throw new IllegalArgumentException("No se pudo leer el archivo");
        }

        String filename = a.getOriginalName();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(a.getMimeType()))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename.replace("\"", "") + "\""
                )
                .body(resource);
    }
}