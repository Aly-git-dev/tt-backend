package com.upiiz.platform_api.repositories;

import com.upiiz.platform_api.entities.ChatConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatConversationRepo extends JpaRepository<ChatConversation, Long> {

    @Query("""
        select c from ChatConversation c
        where (c.user1Id = :a and c.user2Id = :b)
           or (c.user1Id = :b and c.user2Id = :a)
    """)
    Optional<ChatConversation> findDirectByUsers(@Param("a") UUID a, @Param("b") UUID b);

    @Query("""
        select c from ChatConversation c
        where c.user1Id = :me or c.user2Id = :me
        order by c.lastMessageAt desc nulls last, c.id desc
    """)
    List<ChatConversation> listForUser(@Param("me") UUID me);
}