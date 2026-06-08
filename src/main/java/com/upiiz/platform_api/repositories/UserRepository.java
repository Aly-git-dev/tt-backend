package com.upiiz.platform_api.repositories;

import com.upiiz.platform_api.entities.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmailInst(String emailInst);
    Optional<User> findByEmailInstIgnoreCase(String emailInst);
    Optional<User> findByExternalId(String externalId);
    List<User> findByEmailVerifiedTrueAndApprovedFalseAndActiveTrue();
    List<User> findByActiveFalseOrderByNombreAsc();
    List<User> findByActiveFalse();
    @Query("""
    select u.id, u.emailInst, u.nombre
    from User u
    where lower(u.emailInst) like lower(concat('%', :query, '%'))
""")
    List<Object[]> searchByEmail(@Param("query") String query);

    @Query("""
    select distinct u.id, u.emailInst, u.nombre
    from User u
    join u.roles r
    where u.active = true
      and upper(r.name) in :roles
      and (
          lower(coalesce(u.emailInst, '')) like lower(concat('%', :query, '%'))
          or lower(coalesce(u.nombre, '')) like lower(concat('%', :query, '%'))
      )
    order by u.nombre asc
""")
    List<Object[]> searchActiveUsersByRoles(
            @Param("query") String query,
            @Param("roles") Collection<String> roles,
            Pageable pageable
    );
}
