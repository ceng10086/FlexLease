package com.flexlease.user.scheduler;

import com.flexlease.common.user.CreditTier;
import com.flexlease.user.domain.Vendor;
import com.flexlease.user.domain.VendorStatus;
import com.flexlease.user.repository.VendorRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 抽成档位季度评估调度器。
 * <p>
 * 根据厂商的 SLA 评分自动调整信用档位（commissionCreditTier），
 * 从而影响平台抽成比例。评估规则：
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

    public CommissionReviewScheduler(VendorRepository vendorRepository) {
        this.vendorRepository = vendorRepository;
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

        for (Vendor vendor : activeVendors) {
            CreditTier oldTier = vendor.getCommissionCreditTier();
            CreditTier newTier = evaluateTier(vendor.getCommissionSlaScore());

            if (oldTier != newTier) {
                vendor.updateCommissionProfile(
                        vendor.getIndustryCategory(),
                        vendor.getCommissionBaseRate(),
                        newTier,
                        vendor.getCommissionSlaScore()
                );
                vendorRepository.save(vendor);

                if (tierOrdinal(newTier) < tierOrdinal(oldTier)) {
                    upgraded++;
                    LOG.info("Vendor {} upgraded from {} to {} (SLA={})",
                            vendor.getId(), oldTier, newTier, vendor.getCommissionSlaScore());
                } else {
                    downgraded++;
                    LOG.info("Vendor {} downgraded from {} to {} (SLA={})",
                            vendor.getId(), oldTier, newTier, vendor.getCommissionSlaScore());
                }
            } else {
                unchanged++;
            }
        }

        LOG.info("Commission tier review completed: {} upgraded, {} downgraded, {} unchanged",
                upgraded, downgraded, unchanged);
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
}
