package com.flexlease.user.service;

import com.flexlease.common.exception.BusinessException;
import com.flexlease.common.exception.ErrorCode;
import com.flexlease.common.notification.NotificationSendRequest;
import com.flexlease.common.user.CreditTierRules;
import com.flexlease.user.domain.CreditAdjustment;
import com.flexlease.user.domain.CreditEventType;
import com.flexlease.user.domain.UserProfile;
import com.flexlease.user.domain.UserProfileGender;
import com.flexlease.user.dto.CreditAdjustmentResponse;
import com.flexlease.user.dto.PagedResponse;
import com.flexlease.user.dto.UserCreditResponse;
import com.flexlease.user.dto.UserProfileResponse;
import com.flexlease.user.dto.UserProfileUpdateRequest;
import com.flexlease.user.integration.NotificationClient;
import com.flexlease.user.repository.CreditAdjustmentRepository;
import com.flexlease.user.repository.UserProfileRepository;
import java.util.Map;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserProfileService {

    private static final Logger LOG = LoggerFactory.getLogger(UserProfileService.class);

    /**
     * 用户档案与信用分聚合服务。
     * <p>
     * 覆盖：用户资料读写、信用分加载（用于下单试算）、管理员/内部的信用分人工调整与记录。
     */
    private final UserProfileRepository userProfileRepository;
    private final CreditEventService creditEventService;
    private final CreditAdjustmentRepository creditAdjustmentRepository;
    private final NotificationClient notificationClient;

    public UserProfileService(UserProfileRepository userProfileRepository,
                              CreditEventService creditEventService,
                              CreditAdjustmentRepository creditAdjustmentRepository,
                              NotificationClient notificationClient) {
        this.userProfileRepository = userProfileRepository;
        this.creditEventService = creditEventService;
        this.creditAdjustmentRepository = creditAdjustmentRepository;
        this.notificationClient = notificationClient;
    }

    @Transactional
    public UserProfileResponse getOrCreate(UUID userId) {
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseGet(() -> userProfileRepository.save(UserProfile.create(userId)));
        return toResponse(profile);
    }

    @Transactional
    public UserProfileResponse update(UUID userId, UserProfileUpdateRequest request) {
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseGet(() -> UserProfile.create(userId));
        // 这里用“资料补全”模拟一次 KYC 完成：避免引入外部实名接口，满足演示闭环即可。
        boolean eligibleForKyc = !profile.isKycVerified()
                && request.fullName() != null
                && !request.fullName().isBlank()
                && request.phone() != null
                && !request.phone().isBlank();
        profile.updateProfile(
                request.fullName(),
                parseGender(request.gender()),
                request.phone(),
                request.email(),
                request.address()
        );
        UserProfile saved = userProfileRepository.save(profile);
        if (eligibleForKyc) {
            try {
                creditEventService.applyEvent(userId, CreditEventType.KYC_VERIFIED, Map.of("source", "profile"));
                saved = userProfileRepository.findByUserId(userId).orElse(saved);
            } catch (RuntimeException ex) {
                LOG.warn("Failed to grant KYC credit bonus for user {}: {}", userId, ex.getMessage());
            }
        }
        return toResponse(saved);
    }

    @Transactional
    public UserProfileResponse adjustCredit(UUID userId, int delta, String reason, UUID operatorId) {
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseGet(() -> userProfileRepository.save(UserProfile.create(userId)));
        profile.applyCreditDelta(delta);
        UserProfile saved = userProfileRepository.save(profile);
        // 记录人工调整流水，便于后续审计与对用户解释。
        CreditAdjustment adjustment = creditAdjustmentRepository.save(CreditAdjustment.create(
                userId,
                delta,
                normalizeReason(reason),
                operatorId
        ));
        notifyCreditAdjusted(userId, delta, adjustment.getReason(), adjustment.getId(), operatorId);
        LOG.info("Adjusted credit for user {} by {} points, operator={}, reason={}, adjustmentId={}",
                userId, delta, operatorId, adjustment.getReason(), adjustment.getId());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PagedResponse<CreditAdjustmentResponse> listCreditAdjustments(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), Math.max(1, Math.min(size, 100)));
        Page<CreditAdjustment> adjustments = creditAdjustmentRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return new PagedResponse<>(
                adjustments.map(this::toResponse).getContent(),
                adjustments.getNumber() + 1,
                adjustments.getSize(),
                adjustments.getTotalElements(),
                adjustments.getTotalPages()
        );
    }

    @Transactional
    public UserCreditResponse loadCredit(UUID userId) {
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseGet(() -> userProfileRepository.save(UserProfile.create(userId)));
        profile.refreshCreditTier();
        UserProfile saved = userProfileRepository.save(profile);
        return new UserCreditResponse(saved.getUserId(), saved.getCreditScore(), saved.getCreditTier());
    }

    @Transactional(readOnly = true)
    public PagedResponse<UserProfileResponse> list(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), Math.max(1, Math.min(size, 100)), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<UserProfile> profilePage;
        if (keyword != null && !keyword.isBlank()) {
            profilePage = userProfileRepository.findAllByFullNameContainingIgnoreCase(keyword, pageable);
        } else {
            profilePage = userProfileRepository.findAll(pageable);
        }
        return new PagedResponse<>(
                profilePage.map(this::toResponse).getContent(),
                profilePage.getNumber() + 1,
                profilePage.getSize(),
                profilePage.getTotalElements(),
                profilePage.getTotalPages()
        );
    }

    private UserProfileGender parseGender(String gender) {
        try {
            return UserProfileGender.valueOf(gender.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "非法性别取值: " + gender);
        }
    }

    private UserProfileResponse toResponse(UserProfile profile) {
        return new UserProfileResponse(
                profile.getId(),
                profile.getUserId(),
                profile.getFullName(),
                profile.getGender(),
                profile.getPhone(),
                profile.getEmail(),
                profile.getAddress(),
                profile.getCreditScore() == null ? CreditTierRules.defaultScore() : profile.getCreditScore(),
                profile.getCreditTier(),
                profile.isKycVerified(),
                profile.getKycVerifiedAt(),
                profile.getPaymentStreak(),
                profile.getSuspendedUntil(),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }

    private CreditAdjustmentResponse toResponse(CreditAdjustment adjustment) {
        return new CreditAdjustmentResponse(
                adjustment.getId(),
                adjustment.getUserId(),
                adjustment.getDelta(),
                adjustment.getReason(),
                adjustment.getOperatorId(),
                adjustment.getCreatedAt()
        );
    }

    private String normalizeReason(String reason) {
        if (reason == null) {
            return null;
        }
        String normalized = reason.trim();
        return normalized.isBlank() ? null : normalized;
    }

    private void notifyCreditAdjusted(UUID userId,
                                      int delta,
                                      String reason,
                                      UUID adjustmentId,
                                      UUID operatorId) {
        String deltaText = delta >= 0 ? "+" + delta : String.valueOf(delta);
        String subject = operatorId == null ? "信用分调整提醒" : "信用分人工调整";
        String content = "信用积分 %s，原因：%s。".formatted(deltaText, reason == null ? "无" : reason);
        NotificationSendRequest request = new NotificationSendRequest(
                null,
                userId.toString(),
                subject,
                content,
                Map.of("delta", delta, "reason", reason == null ? "" : reason),
                "CREDIT",
                adjustmentId == null ? null : adjustmentId.toString()
        );
        notificationClient.send(request);
    }
}
