package com.flexlease.user.repository;

import com.flexlease.user.domain.VendorApplication;
import com.flexlease.user.domain.VendorApplicationStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VendorApplicationRepository extends JpaRepository<VendorApplication, UUID> {

    Optional<VendorApplication> findByOwnerUserId(UUID ownerUserId);

    boolean existsByUnifiedSocialCode(String unifiedSocialCode);

    @Query("select v from VendorApplication v where (:status is null or v.status = :status) order by v.createdAt desc")
    List<VendorApplication> findAllByStatus(@Param("status") VendorApplicationStatus status);
}
