package com.flexlease.user.service;

import com.flexlease.common.exception.BusinessException;
import com.flexlease.common.exception.ErrorCode;
import com.flexlease.user.domain.VendorApplication;
import com.flexlease.user.domain.VendorApplicationStatus;
import com.flexlease.user.dto.VendorApplicationRequest;
import com.flexlease.user.dto.VendorApplicationResponse;
import com.flexlease.user.repository.VendorApplicationRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VendorApplicationService {

    private final VendorApplicationRepository vendorApplicationRepository;

    public VendorApplicationService(VendorApplicationRepository vendorApplicationRepository) {
        this.vendorApplicationRepository = vendorApplicationRepository;
    }

    @Transactional
    public VendorApplicationResponse submit(UUID ownerUserId, VendorApplicationRequest request) {
        vendorApplicationRepository.findByOwnerUserId(ownerUserId).ifPresent(existing -> {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "已提交厂商申请，请勿重复提交");
        });
        if (vendorApplicationRepository.existsByUnifiedSocialCode(request.unifiedSocialCode())) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "统一社会信用代码已存在");
        }
        VendorApplication application = VendorApplication.submit(
                ownerUserId,
                request.companyName(),
                request.unifiedSocialCode(),
                request.contactName(),
                request.contactPhone(),
                request.contactEmail(),
                request.province(),
                request.city(),
                request.address()
        );
        VendorApplication saved = vendorApplicationRepository.save(application);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public VendorApplicationResponse get(UUID id) {
        return vendorApplicationRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "厂商申请不存在"));
    }

    @Transactional(readOnly = true)
    public List<VendorApplicationResponse> list(VendorApplicationStatus status) {
        return vendorApplicationRepository.findAllByStatus(status).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public VendorApplicationResponse approve(UUID applicationId, UUID reviewerId, String remark) {
        VendorApplication application = vendorApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "厂商申请不存在"));
        application.approve(reviewerId, remark);
        return toResponse(application);
    }

    @Transactional
    public VendorApplicationResponse reject(UUID applicationId, UUID reviewerId, String remark) {
        VendorApplication application = vendorApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "厂商申请不存在"));
        application.reject(reviewerId, remark);
        return toResponse(application);
    }

    private VendorApplicationResponse toResponse(VendorApplication application) {
        return new VendorApplicationResponse(
                application.getId(),
                application.getOwnerUserId(),
                application.getCompanyName(),
                application.getUnifiedSocialCode(),
                application.getContactName(),
                application.getContactPhone(),
                application.getContactEmail(),
                application.getProvince(),
                application.getCity(),
                application.getAddress(),
                application.getStatus(),
                application.getSubmittedAt(),
                application.getReviewedBy(),
                application.getReviewedAt(),
                application.getReviewRemark()
        );
    }
}
