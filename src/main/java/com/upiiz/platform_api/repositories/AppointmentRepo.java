package com.upiiz.platform_api.repositories;

import com.upiiz.platform_api.entities.Appointment;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.*;

public interface AppointmentRepo extends JpaRepository<Appointment, UUID> {

    @Query("""
    select distinct a from Appointment a
      left join a.participants p
    where (a.createdBy = :userId or p.userId = :userId)
      and a.startsAt < :to and a.endsAt > :from
    order by a.startsAt asc
  """)
    List<Appointment> findAgenda(@Param("userId") UUID userId,
                                 @Param("from") LocalDateTime from,
                                 @Param("to") LocalDateTime to);
}
