package com.upiiz.platform_api.repositories;

import com.upiiz.platform_api.entities.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.*;

public interface ChatMessageRepo extends JpaRepository<ChatMessage, Long> {

    @Query("""
      select m from ChatMessage m
      where m.conversationId = :convId
      order by m.createdAt desc
    """)
    List<ChatMessage> pageLatest(@Param("convId") Long convId, Pageable pageable);

    @Query("""
      select m from ChatMessage m
      where m.conversationId = :convId and m.createdAt < :before
      order by m.createdAt desc
    """)
    List<ChatMessage> pageBefore(@Param("convId") Long convId, @Param("before") Instant before, Pageable pageable);

    @Query("""
      select m from ChatMessage m
      where m.conversationId = :convId and m.createdAt <= :reportedAt
      order by m.createdAt desc
    """)
    List<ChatMessage> contextForReport(@Param("convId") Long convId, @Param("reportedAt") Instant reportedAt, Pageable pageable);

    Optional<ChatMessage> findByConversationIdAndSenderIdAndClientMessageId(Long conversationId, UUID senderId, String clientMessageId);
}