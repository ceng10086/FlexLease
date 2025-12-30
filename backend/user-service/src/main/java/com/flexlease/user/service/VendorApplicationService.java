package com.flexlease.user.service;

import com.flexlease.common.exception.BusinessException;
import com.flexlease.common.exception.ErrorCode;
import com.flexlease.user.domain.VendorApplication;
import com.flexlease.user.domain.VendorApplicationStatus;
import com.flexlease.user.dto.VendorApplicationRequest;
import com.flexlease.user.dto.VendorApplicationResponse;
import com.flexlease.user.integration.AuthServiceClient;
import com.flexlease.user.repository.VendorApplicationRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VendorApplicationService {

    private final VendorApplicationRepository vendorApplicationRepository;
    private final AuthServiceClient authServiceClient;
    private final VendorService vendorService;

    public VendorApplicationService(VendorApplicationRepository vendorApplicationRepository,
                                    AuthServiceClient authServiceClient,
                                    VendorService vendorService) {
        this.vendorApplicationRepository = vendorApplicationRepository;
        this.authServiceClient = authServiceClient;
        this.vendorService = vendorService;
    }

    @Transactional
    public VendorApplicationResponse submit(UUID ownerUserId, VendorApplicationRequest request) {
        return vendorApplicationRepository.findByOwnerUserId(ownerUserId)
                .map(existing -> handleExistingApplication(existing, request))
                .orElseGet(() -> createNewApplication(ownerUserId, request));
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

    @Transactional(readOnly = true)
    public List<VendorApplicationResponse> listForOwner(UUID ownerUserId, VendorApplicationStatus status) {
        return vendorApplicationRepository.findAllByOwnerUserId(ownerUserId).stream()
                .filter(application -> status == null || application.getStatus() == status)
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public VendorApplicationResponse approve(UUID applicationId, UUID reviewerId, String remark) {
        VendorApplication application = vendorApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "厂商申请不存在"));
        if (application.getStatus() != VendorApplicationStatus.SUBMITTED) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "仅允许审核状态为 SUBMITTED 的申请");
        }
        application.approve(reviewerId, remark);
        authServiceClient.activateAccount(application.getOwnerUserId());
        var vendor = vendorService.ensureVendorForApplication(application);
        authServiceClient.assignVendor(application.getOwnerUserId(), vendor.id());
        return toResponse(application);
    }

    @Transactional
    public VendorApplicationResponse reject(UUID applicationId, UUID reviewerId, String remark) {
        VendorApplication application = vendorApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "厂商申请不存在"));
        if (application.getStatus() != VendorApplicationStatus.SUBMITTED) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "仅允许驳回状态为 SUBMITTED 的申请");
        }
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

    private VendorApplicationResponse handleExistingApplication(VendorApplication existing, VendorApplicationRequest request) {
        if (existing.getStatus() == VendorApplicationStatus.SUBMITTED) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "申请正在审核中，请勿重复提交");
        }
        boolean codeChanged = request.unifiedSocialCode() != null
                && !request.unifiedSocialCode().equalsIgnoreCase(existing.getUnifiedSocialCode());
        if (codeChanged && vendorApplicationRepository.existsByUnifiedSocialCode(request.unifiedSocialCode())) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "统一社会信用代码已存在");
        }
        existing.resubmit(
                request.companyName(),
                request.unifiedSocialCode(),
                request.contactName(),
                request.contactPhone(),
                request.contactEmail(),
                request.province(),
                request.city(),
                request.address()
        );
        return toResponse(existing);
    }

    private VendorApplicationResponse createNewApplication(UUID ownerUserId, VendorApplicationRequest request) {
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
}
