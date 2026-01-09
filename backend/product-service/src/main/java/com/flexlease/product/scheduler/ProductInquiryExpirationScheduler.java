package com.flexlease.product.scheduler;

import com.flexlease.product.domain.ProductInquiryStatus;
import com.flexlease.product.repository.ProductInquiryRepository;
import java.time.OffsetDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 商品咨询过期调度器。
 * <p>
 * 定期将超过有效期且仍处于 OPEN 的咨询批量置为 EXPIRED，避免咨询列表长期积压未关闭记录。
 */
@Component
public class ProductInquiryExpirationScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(ProductInquiryExpirationScheduler.class);

    private final ProductInquiryRepository productInquiryRepository;

    public ProductInquiryExpirationScheduler(ProductInquiryRepository productInquiryRepository) {
        this.productInquiryRepository = productInquiryRepository;
    }

    @Scheduled(
            initialDelayString = "${flexlease.product.inquiry-expiration.initial-delay-ms:600000}",
            fixedDelayString = "${flexlease.product.inquiry-expiration.fixed-delay-ms:3600000}"
    )
    public void expireInquiries() {
        int updated = productInquiryRepository.bulkExpire(
                ProductInquiryStatus.OPEN,
                ProductInquiryStatus.EXPIRED,
                OffsetDateTime.now()
        );
        if (updated > 0) {
            LOG.info("Expired {} product inquiries", updated);
        }
    }
}
