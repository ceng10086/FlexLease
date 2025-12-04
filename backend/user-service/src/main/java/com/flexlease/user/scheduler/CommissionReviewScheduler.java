package com.flexlease.user.scheduler;

import com.flexlease.common.notification.NotificationChannel;
import com.flexlease.common.notification.NotificationSendRequest;
import com.flexlease.common.user.CreditTier;
import com.flexlease.user.domain.Vendor;
import com.flexlease.user.domain.VendorStatus;
import com.flexlease.user.integration.NotificationClient;
import com.flexlease.user.repository.VendorRepository;
import com.flexlease.user.service.VendorSlaCalculationService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 抽成档位季度评估调度器。
 * <p>
 * 功能：
 * <ol>
 *   <li>自动计算厂商 SLA 评分（基于订单履约数据）</li>
 *   <li>根据 SLA 评分调整信用档位（commissionCreditTier）</li>
 *   <li>通知厂商费率变更</li>
 * </ol>
 * 评估规则：
 * <ul>
 *   <li>SLA ≥ 95：升为 EXCELLENT（抽成减 2%）</li>
 *   <li>SLA ≥ 85：维持/升为 STANDARD</li>
 *   <li>SLA ≥ 70：降为 WARNING（抽成加 2%）</li>
 *   <li>SLA < 70：降为 RESTRICTED（抽成加 3%）</li>
 * </ul>
 * 默认每季度执行一次（可通过 cron 配置覆盖）。
 */
@Component
public class CommissionReviewScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(CommissionReviewScheduler.class);

    private static final int EXCELLENT_THRESHOLD = 95;
    private static final int STANDARD_THRESHOLD = 85;
    private static final int WARNING_THRESHOLD = 70;

    private final VendorRepository vendorRepository;
    private final VendorSlaCalculationService slaCalculationService;
    private final NotificationClient notificationClient;

    public CommissionReviewScheduler(VendorRepository vendorRepository,
                                     VendorSlaCalculationService slaCalculationService,
                                     NotificationClient notificationClient) {
        this.vendorRepository = vendorRepository;
        this.slaCalculationService = slaCalculationService;
        this.notificationClient = notificationClient;
    }

    /**
     * 季度执行抽成档位评估。
     * 默认在每个季度首月 1 日凌晨 3 点运行。
     */
    @Scheduled(cron = "${flexlease.commission.review-cron:0 0 3 1 1,4,7,10 *}")
    @Transactional
    public void reviewCommissionTiers() {
        LOG.info("Starting quarterly commission tier review...");
        List<Vendor> activeVendors = vendorRepository.findByStatus(VendorStatus.ACTIVE);
        int upgraded = 0;
        int downgraded = 0;
        int unchanged = 0;
        int slaUpdated = 0;

        for (Vendor vendor : activeVendors) {
            // 1. 自动计算 SLA 评分
            Integer calculatedSla = slaCalculationService.calculateSlaScore(vendor.getId());
            if (calculatedSla != null && !calculatedSla.equals(vendor.getCommissionSlaScore())) {
                LOG.info("Vendor {} SLA updated: {} -> {}",
                        vendor.getId(), vendor.getCommissionSlaScore(), calculatedSla);
                slaUpdated++;
            }
            int effectiveSla = calculatedSla != null ? calculatedSla : vendor.getCommissionSlaScore();

            // 2. 评估信用档位
            CreditTier oldTier = vendor.getCommissionCreditTier();
            BigDecimal oldRate = vendor.calculateCommissionRate();
            CreditTier newTier = evaluateTier(effectiveSla);

            if (oldTier != newTier || (calculatedSla != null && !calculatedSla.equals(vendor.getCommissionSlaScore()))) {
                vendor.updateCommissionProfile(
                        vendor.getIndustryCategory(),
                        vendor.getCommissionBaseRate(),
                        newTier,
                        calculatedSla != null ? calculatedSla : vendor.getCommissionSlaScore()
                );
                vendorRepository.save(vendor);

                BigDecimal newRate = vendor.calculateCommissionRate();

                if (oldTier != newTier) {
                    if (tierOrdinal(newTier) < tierOrdinal(oldTier)) {
                        upgraded++;
                        LOG.info("Vendor {} upgraded from {} to {} (SLA={})",
                                vendor.getId(), oldTier, newTier, effectiveSla);
                    } else {
                        downgraded++;
                        LOG.info("Vendor {} downgraded from {} to {} (SLA={})",
                                vendor.getId(), oldTier, newTier, effectiveSla);
                    }

                    // 3. 通知费率变更
                    notifyCommissionRateChange(vendor, oldTier, newTier, oldRate, newRate, effectiveSla);
                }
            } else {
                unchanged++;
            }
        }

        LOG.info("Commission tier review completed: {} upgraded, {} downgraded, {} unchanged, {} SLA updated",
                upgraded, downgraded, unchanged, slaUpdated);
    }

    /**
     * 根据 SLA 评分评估信用档位。
     */
    private CreditTier evaluateTier(Integer slaScore) {
        int score = slaScore != null ? slaScore : 80;
        if (score >= EXCELLENT_THRESHOLD) {
            return CreditTier.EXCELLENT;
        } else if (score >= STANDARD_THRESHOLD) {
            return CreditTier.STANDARD;
        } else if (score >= WARNING_THRESHOLD) {
            return CreditTier.WARNING;
        } else {
            return CreditTier.RESTRICTED;
        }
    }

    /**
     * 计算档位序号（EXCELLENT=0, STANDARD=1, WARNING=2, RESTRICTED=3）。
     */
    private int tierOrdinal(CreditTier tier) {
        return switch (tier) {
            case EXCELLENT -> 0;
            case STANDARD -> 1;
            case WARNING -> 2;
            case RESTRICTED -> 3;
        };
    }

    /**
     * 通知厂商抽成费率变更。
     */
    private void notifyCommissionRateChange(Vendor vendor,
                                            CreditTier oldTier,
                                            CreditTier newTier,
                                            BigDecimal oldRate,
                                            BigDecimal newRate,
                                            int slaScore) {
        String direction = tierOrdinal(newTier) < tierOrdinal(oldTier) ? "提升" : "下调";
        String rateChange = formatRatePercent(oldRate) + " → " + formatRatePercent(newRate);
        String content = String.format(
                "尊敬的厂商，您的季度履约评估已完成。\n" +
                "SLA 评分：%d 分\n" +
                "信用档位：%s → %s（%s）\n" +
                "抽成费率：%s\n" +
                "新费率将于下一结算周期生效。感谢您持续提供优质服务！",
                slaScore,
                tierLabel(oldTier), tierLabel(newTier), direction,
                rateChange
        );

        NotificationSendRequest request = new NotificationSendRequest(
                null,
                NotificationChannel.IN_APP,
                vendor.getOwnerUserId().toString(),
                "季度抽成费率调整通知",
                content,
                Map.of(
                        "vendorId", vendor.getId().toString(),
                        "oldTier", oldTier.name(),
                        "newTier", newTier.name(),
                        "slaScore", String.valueOf(slaScore)
                ),
                "VENDOR",
                vendor.getId().toString()
        );

        try {
            notificationClient.send(request);
            LOG.debug("Commission rate change notification sent to vendor {}", vendor.getId());
        } catch (RuntimeException ex) {
            LOG.warn("Failed to notify vendor {} about commission rate change: {}",
                    vendor.getId(), ex.getMessage());
        }
    }

    private String tierLabel(CreditTier tier) {
        return switch (tier) {
            case EXCELLENT -> "优秀";
            case STANDARD -> "标准";
            case WARNING -> "警告";
            case RESTRICTED -> "受限";
        };
    }

    private String formatRatePercent(BigDecimal rate) {
        return rate.multiply(BigDecimal.valueOf(100)).setScale(2, java.math.RoundingMode.HALF_UP) + "%";
    }
}
