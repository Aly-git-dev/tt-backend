package com.upiiz.platform_api.repositories;

import com.upiiz.platform_api.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface UserRoleNativeRepo extends JpaRepository<User, UUID> {

    @Query(value = """
        SELECT r.name
        FROM roles r
        JOIN user_roles ur ON ur.role_id = r.id
        WHERE ur.user_id = :uid
    """, nativeQuery = true)
    List<String> roleNames(@Param("uid") UUID userId);

    @Query(value = """
        SELECT ur.user_id
        FROM user_roles ur
        JOIN roles r ON r.id = ur.role_id
        WHERE UPPER(r.name) = UPPER(:roleName)
    """, nativeQuery = true)
    List<UUID> findUserIdsByRoleName(@Param("roleName") String roleName);
}