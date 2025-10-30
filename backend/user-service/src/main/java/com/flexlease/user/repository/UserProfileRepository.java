package com.flexlease.user.repository;

import com.flexlease.user.domain.UserProfile;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {

    Optional<UserProfile> findByUserId(UUID userId);

    Page<UserProfile> findAllByFullNameContainingIgnoreCase(String keyword, Pageable pageable);
}
