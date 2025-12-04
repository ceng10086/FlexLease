package com.flexlease.user.scheduler;

import com.flexlease.common.notification.NotificationChannel;
import com.flexlease.common.notification.NotificationSendRequest;
import com.flexlease.user.domain.UserProfile;
import com.flexlease.user.integration.AuthServiceClient;
import com.flexlease.user.integration.NotificationClient;
import com.flexlease.user.repository.UserProfileRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 账号自动解冻调度器。
 * <p>
 * 每小时检查一次需要解冻的账号，自动恢复其状态并通知用户。
 */
@Component
public class AccountUnfreezeScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(AccountUnfreezeScheduler.class);

    private final UserProfileRepository userProfileRepository;
    private final AuthServiceClient authServiceClient;
    private final NotificationClient notificationClient;

    public AccountUnfreezeScheduler(UserProfileRepository userProfileRepository,
                                    AuthServiceClient authServiceClient,
                                    NotificationClient notificationClient) {
        this.userProfileRepository = userProfileRepository;
        this.authServiceClient = authServiceClient;
        this.notificationClient = notificationClient;
    }

    /**
     * 每小时执行一次账号解冻检查。
     */
    @Scheduled(cron = "${flexlease.account.unfreeze-cron:0 0 * * * *}")
    @Transactional
    public void unfreezeExpiredAccounts() {
        LOG.info("Starting account unfreeze check...");
        OffsetDateTime now = OffsetDateTime.now();
        List<UserProfile> suspendedProfiles = userProfileRepository.findBySuspendedUntilBeforeAndSuspendedUntilIsNotNull(now);

        int unfrozenCount = 0;
        for (UserProfile profile : suspendedProfiles) {
            try {
                // 恢复账号状态
                authServiceClient.activateAccount(profile.getUserId());
                profile.clearSuspension();
                userProfileRepository.save(profile);

                // 通知用户
                notifyAccountUnfrozen(profile);
                unfrozenCount++;
                LOG.info("User {} account has been unfrozen", profile.getUserId());
            } catch (RuntimeException ex) {
                LOG.warn("Failed to unfreeze user {} account: {}", profile.getUserId(), ex.getMessage());
            }
        }

        LOG.info("Account unfreeze check completed: {} accounts unfrozen", unfrozenCount);
    }

    private void notifyAccountUnfrozen(UserProfile profile) {
        NotificationSendRequest request = new NotificationSendRequest(
                null,
                NotificationChannel.IN_APP,
                profile.getUserId().toString(),
                "账号解冻通知",
                "您的账号冻结期已满，现已自动解冻。请遵守平台规则，祝您使用愉快。",
                Map.of(),
                "ACCOUNT",
                null
        );
        try {
            notificationClient.send(request);
        } catch (RuntimeException ex) {
            LOG.warn("Failed to notify user {} about account unfreeze: {}", profile.getUserId(), ex.getMessage());
        }
    }
}
