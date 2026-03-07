package com.upiiz.platform_api.repositories;

import com.upiiz.platform_api.entities.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.*;

public interface ChatConversationRepo extends JpaRepository<ChatConversation, Long> {

    @Query("""
      select c from ChatConversation c
      where least(c.user1Id, c.user2Id) = least(:a, :b)
        and greatest(c.user1Id, c.user2Id) = greatest(:a, :b)
    """)
    Optional<ChatConversation> findDirectByUsers(@Param("a") UUID a, @Param("b") UUID b);

    @Query("""
      select c from ChatConversation c
      where c.user1Id = :me or c.user2Id = :me
      order by c.lastMessageAt desc nulls last, c.id desc
    """)
    List<ChatConversation> listForUser(@Param("me") UUID me);
}