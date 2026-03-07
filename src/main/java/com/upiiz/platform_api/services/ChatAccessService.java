package com.upiiz.platform_api.services;

import com.upiiz.platform_api.entities.ChatConversation;
import com.upiiz.platform_api.repositories.ChatConversationRepo;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ChatAccessService {

    private final ChatConversationRepo convRepo;

    public ChatAccessService(ChatConversationRepo convRepo) {
        this.convRepo = convRepo;
    }

    public ChatConversation requireConversation(Long conversationId) {
        return convRepo.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));
    }

    public void requireParticipant(ChatConversation c, UUID userId) {
        if (!(userId.equals(c.getUser1Id()) || userId.equals(c.getUser2Id()))) {
            throw new SecurityException("Not a participant");
        }
    }

    public UUID otherUser(ChatConversation c, UUID me) {
        if (me.equals(c.getUser1Id())) return c.getUser2Id();
        if (me.equals(c.getUser2Id())) return c.getUser1Id();
        throw new SecurityException("Not a participant");
    }
}
