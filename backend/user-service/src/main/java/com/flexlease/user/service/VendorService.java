package com.flexlease.user.service;

import com.flexlease.common.exception.BusinessException;
import com.flexlease.common.exception.ErrorCode;
import com.flexlease.user.domain.Vendor;
import com.flexlease.user.domain.VendorApplication;
import com.flexlease.user.domain.VendorStatus;
import com.flexlease.user.dto.PagedResponse;
import com.flexlease.user.dto.VendorCommissionProfileRequest;
import com.flexlease.user.dto.VendorCommissionProfileResponse;
import com.flexlease.user.dto.VendorResponse;
import com.flexlease.user.dto.VendorUpdateRequest;
import com.flexlease.user.repository.VendorRepository;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VendorService {

    /**
     * 厂商资料服务。
     * <p>
     * 厂商主体在“入驻审核通过”时创建；后续可由管理员维护抽成档案，也可由厂商维护联系信息等资料。
     */
    private final VendorRepository vendorRepository;

    public VendorService(VendorRepository vendorRepository) {
        this.vendorRepository = vendorRepository;
    }

    @Transactional
    public VendorResponse ensureVendorForApplication(VendorApplication application) {
        Vendor vendor = vendorRepository.findByOwnerUserId(application.getOwnerUserId())
                .orElseGet(() -> Vendor.create(
                        application.getOwnerUserId(),
                        application.getCompanyName(),
                        application.getContactName(),
                        application.getContactPhone(),
                        application.getContactEmail(),
                        application.getProvince(),
                        application.getCity(),
                        application.getAddress()
                ));
        vendor.updateCompanyName(application.getCompanyName());
        vendor.updateContactInfo(
                application.getContactName(),
                application.getContactPhone(),
                application.getContactEmail(),
                application.getProvince(),
                application.getCity(),
                application.getAddress()
        );
        Vendor saved = vendorRepository.save(vendor);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PagedResponse<VendorResponse> list(VendorStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), Math.max(1, Math.min(size, 100)), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Vendor> vendorPage = status == null
                ? vendorRepository.findAll(pageable)
                : vendorRepository.findAllByStatus(status, pageable);
        return new PagedResponse<>(
                vendorPage.map(this::toResponse).getContent(),
                vendorPage.getNumber() + 1,
                vendorPage.getSize(),
                vendorPage.getTotalElements(),
                vendorPage.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public VendorResponse get(UUID vendorId) {
        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "厂商不存在"));
        return toResponse(vendor);
    }

    @Transactional
    public VendorResponse update(UUID vendorId, VendorUpdateRequest request) {
        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "厂商不存在"));
        vendor.updateContactInfo(
                request.contactName(),
                request.contactPhone(),
                request.contactEmail(),
                request.province(),
                request.city(),
                request.address()
        );
        return toResponse(vendor);
    }

    @Transactional
    public VendorResponse updateStatus(UUID vendorId, String statusCode) {
        VendorStatus status = parseStatus(statusCode);
        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "厂商不存在"));
        vendor.updateStatus(status);
        return toResponse(vendor);
    }

    private VendorStatus parseStatus(String statusCode) {
        try {
            return VendorStatus.valueOf(statusCode.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "非法状态值: " + statusCode);
        }
    }

    @Transactional
    public VendorCommissionProfileResponse updateCommissionProfile(UUID vendorId,
                                                                   VendorCommissionProfileRequest request) {
        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "厂商不存在"));
        vendor.updateCommissionProfile(
                request.industryCategory(),
                request.baseRate(),
                request.creditTier(),
                request.slaScore()
        );
        return toCommissionProfile(vendor);
    }

    @Transactional(readOnly = true)
    public VendorCommissionProfileResponse loadCommissionProfile(UUID vendorId) {
        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "厂商不存在"));
        return toCommissionProfile(vendor);
    }

    private VendorResponse toResponse(Vendor vendor) {
        return new VendorResponse(
                vendor.getId(),
                vendor.getOwnerUserId(),
                vendor.getCompanyName(),
                vendor.getContactName(),
                vendor.getContactPhone(),
                vendor.getContactEmail(),
                vendor.getProvince(),
                vendor.getCity(),
                vendor.getAddress(),
                vendor.getStatus(),
                vendor.getCreatedAt(),
                vendor.getUpdatedAt(),
                toCommissionProfile(vendor)
        );
    }

    private VendorCommissionProfileResponse toCommissionProfile(Vendor vendor) {
        return new VendorCommissionProfileResponse(
                vendor.getIndustryCategory(),
                vendor.getCommissionBaseRate(),
                vendor.getCommissionCreditTier(),
                vendor.getCommissionSlaScore(),
                vendor.calculateCommissionRate()
        );
    }
}
