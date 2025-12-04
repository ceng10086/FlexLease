package com.flexlease.user.repository;

import com.flexlease.user.domain.UserProfile;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {

    Optional<UserProfile> findByUserId(UUID userId);

    Page<UserProfile> findAllByFullNameContainingIgnoreCase(String keyword, Pageable pageable);

    /**
     * 查找冻结截止时间早于指定时间的账号（用于自动解冻）。
     */
    List<UserProfile> findBySuspendedUntilBeforeAndSuspendedUntilIsNotNull(OffsetDateTime before);
}
