package com.flexlease.product.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.flexlease.common.exception.BusinessException;
import com.flexlease.product.domain.Product;
import com.flexlease.product.domain.ProductInquiry;
import com.flexlease.product.dto.ProductInquiryReplyRequest;
import com.flexlease.product.integration.NotificationClient;
import com.flexlease.product.repository.ProductInquiryRepository;
import com.flexlease.product.repository.ProductRepository;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductInquiryServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductInquiryRepository productInquiryRepository;

    @Mock
    private NotificationClient notificationClient;

    private ProductInquiryService productInquiryService;

    @BeforeEach
    void setUp() {
        productInquiryService = new ProductInquiryService(productRepository, productInquiryRepository, notificationClient);
    }

    @Test
    void replyShouldRejectExpiredInquiry() {
        UUID vendorId = UUID.randomUUID();
        Product product = Product.create(vendorId, "Test", "CAT-01", "desc", null);
        ProductInquiry inquiry = ProductInquiry.create(
                product,
                vendorId,
                UUID.randomUUID(),
                "Alice",
                "123456789",
                "Hi",
                OffsetDateTime.now().minusHours(1)
        );
        UUID inquiryId = inquiry.getId();
        when(productInquiryRepository.findByIdAndVendorId(inquiryId, vendorId)).thenReturn(Optional.of(inquiry));

        assertThatThrownBy(() -> productInquiryService.reply(vendorId, inquiryId, new ProductInquiryReplyRequest("reply")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("咨询已过期");

        verify(productInquiryRepository).save(inquiry);
        verifyNoInteractions(notificationClient);
    }
}
