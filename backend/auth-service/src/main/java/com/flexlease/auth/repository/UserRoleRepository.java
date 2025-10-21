package com.flexlease.auth.repository;

import com.flexlease.auth.domain.UserRole;
import com.flexlease.auth.domain.UserRoleId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {

    List<UserRole> findByIdUserId(java.util.UUID userId);
}
