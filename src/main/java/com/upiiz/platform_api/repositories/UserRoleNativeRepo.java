package com.upiiz.platform_api.repositories;

import com.upiiz.platform_api.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface UserRoleNativeRepo extends JpaRepository<User, UUID> {

    @Query(value = """
        select r.name
        from roles r
        join user_roles ur on ur.role_id = r.id
        where ur.user_id = :uid
    """, nativeQuery = true)
    List<String> roleNames(@Param("uid") UUID userId);
}
