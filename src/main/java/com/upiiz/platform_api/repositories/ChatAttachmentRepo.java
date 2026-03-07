package com.upiiz.platform_api.repositories;

import com.upiiz.platform_api.entities.*;
import org.springframework.data.jpa.repository.*;

import java.util.*;

public interface ChatAttachmentRepo extends JpaRepository<ChatAttachment, Long> {
    List<ChatAttachment> findByMessageIdIn(Collection<Long> messageIds);
    List<ChatAttachment> findByMessageId(Long messageId);
}
