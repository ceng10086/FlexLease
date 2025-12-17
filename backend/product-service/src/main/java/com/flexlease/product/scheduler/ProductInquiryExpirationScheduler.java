package com.flexlease.product.scheduler;

import com.flexlease.product.domain.ProductInquiryStatus;
import com.flexlease.product.repository.ProductInquiryRepository;
import java.time.OffsetDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

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

