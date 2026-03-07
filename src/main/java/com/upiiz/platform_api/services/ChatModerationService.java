package com.upiiz.platform_api.services;

import com.upiiz.platform_api.repositories.ChatSanctionRepo;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class ChatModerationService {

    private final ChatSanctionRepo sanctionRepo;

    public ChatModerationService(ChatSanctionRepo sanctionRepo) {
        this.sanctionRepo = sanctionRepo;
    }

    public void assertUserCanChat(UUID userId) {
        var active = sanctionRepo.activeSanctions(userId, Instant.now());
        boolean blocked = active.stream().anyMatch(s ->
                "BAN".equals(s.getType()) || "TEMP_BLOCK".equals(s.getType())
        );
        if (blocked) throw new SecurityException("User is blocked from chat");
    }
}

