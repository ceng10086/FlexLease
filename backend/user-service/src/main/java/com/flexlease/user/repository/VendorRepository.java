package com.flexlease.user.repository;

import com.flexlease.user.domain.Vendor;
import com.flexlease.user.domain.VendorStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VendorRepository extends JpaRepository<Vendor, UUID> {

    Optional<Vendor> findByOwnerUserId(UUID ownerUserId);

    Page<Vendor> findAllByStatus(VendorStatus status, Pageable pageable);

    List<Vendor> findByStatus(VendorStatus status);
}

