package com.upiiz.platform_api.repositories;

import com.upiiz.platform_api.entities.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.*;

public interface ChatSanctionRepo extends JpaRepository<ChatSanction, Long> {

    @Query("""
      select s from ChatSanction s
      where s.targetUserId = :u
        and (s.endAt is null or s.endAt > :now)
        and s.startAt <= :now
    """)
    List<ChatSanction> activeSanctions(@Param("u") UUID userId, @Param("now") Instant now);
}
