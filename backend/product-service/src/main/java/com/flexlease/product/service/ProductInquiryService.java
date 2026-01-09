package com.flexlease.product.service;

import com.flexlease.common.exception.BusinessException;
import com.flexlease.common.exception.ErrorCode;
import com.flexlease.common.notification.NotificationSendRequest;
import com.flexlease.product.domain.Product;
import com.flexlease.product.domain.ProductInquiry;
import com.flexlease.product.domain.ProductInquiryStatus;
import com.flexlease.product.dto.ProductInquiryReplyRequest;
import com.flexlease.product.dto.ProductInquiryRequest;
import com.flexlease.product.dto.ProductInquiryResponse;
import com.flexlease.product.integration.NotificationClient;
import com.flexlease.product.repository.ProductInquiryRepository;
import com.flexlease.product.repository.ProductRepository;
import jakarta.transaction.Transactional;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class ProductInquiryService {

    /**
     * 商品咨询服务。
     * <p>
     * 咨询默认 72 小时有效；厂商回复前会再次校验是否过期，避免“过期后仍可回复”的边界问题。
     */
    private static final Duration EXPIRE_WINDOW = Duration.ofHours(72);

    private final ProductRepository productRepository;
    private final ProductInquiryRepository productInquiryRepository;
    private final NotificationClient notificationClient;

    public ProductInquiryService(ProductRepository productRepository,
                                 ProductInquiryRepository productInquiryRepository,
                                 NotificationClient notificationClient) {
        this.productRepository = productRepository;
        this.productInquiryRepository = productInquiryRepository;
        this.notificationClient = notificationClient;
    }

    public ProductInquiryResponse createInquiry(UUID productId,
                                                UUID requesterId,
                                                ProductInquiryRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "商品不存在"));
        ProductInquiry inquiry = ProductInquiry.create(
                product,
                product.getVendorId(),
                requesterId,
                request.contactName(),
                request.contactMethod(),
                request.message().trim(),
                OffsetDateTime.now().plus(EXPIRE_WINDOW)
        );
        ProductInquiry saved = productInquiryRepository.save(inquiry);
        notifyVendorNewInquiry(product, saved);
        return toResponse(saved);
    }

    public List<ProductInquiryResponse> listByVendor(UUID vendorId, ProductInquiryStatus status) {
        List<ProductInquiry> inquiries = productInquiryRepository.findByVendor(vendorId, status);
        for (ProductInquiry inquiry : inquiries) {
            ProductInquiryStatus previous = inquiry.getStatus();
            inquiry.refreshExpirationState();
            if (previous != inquiry.getStatus()) {
                productInquiryRepository.save(inquiry);
            }
        }
        return inquiries.stream()
                .map(this::toResponse)
                .toList();
    }

    public List<ProductInquiryResponse> listByRequester(UUID productId, UUID requesterId) {
        List<ProductInquiry> inquiries = productInquiryRepository.findByRequester(productId, requesterId);
        for (ProductInquiry inquiry : inquiries) {
            ProductInquiryStatus previous = inquiry.getStatus();
            inquiry.refreshExpirationState();
            if (previous != inquiry.getStatus()) {
                productInquiryRepository.save(inquiry);
            }
        }
        return inquiries.stream()
                .map(this::toResponse)
                .toList();
    }

    public ProductInquiryResponse reply(UUID vendorId,
                                        UUID inquiryId,
                                        ProductInquiryReplyRequest request) {
        ProductInquiry inquiry = productInquiryRepository.findByIdAndVendorId(inquiryId, vendorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "咨询不存在或已失效"));
        ProductInquiryStatus previous = inquiry.getStatus();
        inquiry.refreshExpirationState();
        if (inquiry.getStatus() == ProductInquiryStatus.EXPIRED) {
            if (previous != ProductInquiryStatus.EXPIRED) {
                productInquiryRepository.save(inquiry);
            }
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "咨询已过期，无法回复");
        }
        inquiry.reply(request.reply().trim());
        ProductInquiry saved = productInquiryRepository.save(inquiry);
        notifyRequesterReply(saved);
        return toResponse(saved);
    }

    private void notifyVendorNewInquiry(Product product, ProductInquiry inquiry) {
        NotificationSendRequest request = new NotificationSendRequest(
                null,
                inquiry.getVendorId().toString(),
                "收到新的商品咨询",
                "商品 %s 收到新的咨询，请尽快答复。".formatted(product.getName()),
                Map.of("productId", product.getId().toString(), "inquiryId", inquiry.getId().toString()),
                "INQUIRY",
                inquiry.getId().toString()
        );
        notificationClient.send(request);
    }

    private void notifyRequesterReply(ProductInquiry inquiry) {
        if (inquiry.getRequesterId() == null) {
            return;
        }
        NotificationSendRequest request = new NotificationSendRequest(
                null,
                inquiry.getRequesterId().toString(),
                "厂商回复了咨询",
                "您的咨询已有回复，请前往订单前沟通面板查看详情。",
                Map.of("inquiryId", inquiry.getId().toString()),
                "INQUIRY",
                inquiry.getId().toString()
        );
        notificationClient.send(request);
    }

    private ProductInquiryResponse toResponse(ProductInquiry inquiry) {
        return new ProductInquiryResponse(
                inquiry.getId(),
                inquiry.getProduct().getId(),
                inquiry.getVendorId(),
                inquiry.getRequesterId(),
                inquiry.getContactName(),
                inquiry.getContactMethod(),
                inquiry.getMessage(),
                inquiry.getStatus(),
                inquiry.getReply(),
                inquiry.getExpiresAt(),
                inquiry.getRespondedAt(),
                inquiry.getCreatedAt()
        );
    }
}
