package com.flexlease.user.service;

import com.flexlease.common.exception.BusinessException;
import com.flexlease.common.exception.ErrorCode;
import com.flexlease.common.user.CreditTierRules;
import com.flexlease.user.domain.CreditEventType;
import com.flexlease.user.domain.UserProfile;
import com.flexlease.user.domain.UserProfileGender;
import com.flexlease.user.dto.PagedResponse;
import com.flexlease.user.dto.UserCreditResponse;
import com.flexlease.user.dto.UserProfileResponse;
import com.flexlease.user.dto.UserProfileUpdateRequest;
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

    private final UserProfileRepository userProfileRepository;
    private final CreditEventService creditEventService;

    public UserProfileService(UserProfileRepository userProfileRepository,
                              CreditEventService creditEventService) {
        this.userProfileRepository = userProfileRepository;
        this.creditEventService = creditEventService;
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
        LOG.info("Adjusted credit for user {} by {} points, operator={}, reason={}",
                userId, delta, operatorId, reason);
        return toResponse(saved);
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
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }
}
