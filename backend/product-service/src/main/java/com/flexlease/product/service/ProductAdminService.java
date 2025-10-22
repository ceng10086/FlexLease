package com.flexlease.product.service;

import com.flexlease.common.exception.BusinessException;
import com.flexlease.common.exception.ErrorCode;
import com.flexlease.product.domain.Product;
import com.flexlease.product.domain.ProductStatus;
import com.flexlease.product.dto.PagedResponse;
import com.flexlease.product.dto.ProductResponse;
import com.flexlease.product.dto.ProductSummaryResponse;
import com.flexlease.product.repository.ProductRepository;
import jakarta.transaction.Transactional;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class ProductAdminService {

    private final ProductRepository productRepository;
    private final ProductAssembler assembler;

    public ProductAdminService(ProductRepository productRepository, ProductAssembler assembler) {
        this.productRepository = productRepository;
        this.assembler = assembler;
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public PagedResponse<ProductSummaryResponse> listProducts(ProductStatus status, String keyword, Pageable pageable) {
        ProductStatus targetStatus = status != null ? status : ProductStatus.PENDING_REVIEW;
        Page<Product> page;
        boolean hasKeyword = StringUtils.hasText(keyword);
        if (hasKeyword) {
            page = productRepository.findByStatusAndNameContainingIgnoreCase(targetStatus, keyword, pageable);
        } else {
            page = productRepository.findByStatus(targetStatus, pageable);
        }
        return toPagedResponse(page, assembler::toSummary);
    }

    public ProductResponse approveProduct(UUID productId, UUID reviewerId, String remark) {
        Product product = loadProduct(productId);
        if (product.getStatus() != ProductStatus.PENDING_REVIEW) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "仅待审核商品可执行该操作");
        }
        product.markApproved(reviewerId, remark);
        return assembler.toProductResponse(product);
    }

    public ProductResponse rejectProduct(UUID productId, UUID reviewerId, String remark) {
        Product product = loadProduct(productId);
        if (product.getStatus() != ProductStatus.PENDING_REVIEW) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "仅待审核商品可执行该操作");
        }
        product.markRejected(reviewerId, remark);
        return assembler.toProductResponse(product);
    }

    private Product loadProduct(UUID productId) {
        return productRepository.findWithPlansById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "商品不存在"));
    }

    private <T, R> PagedResponse<R> toPagedResponse(Page<T> page, Function<T, R> mapper) {
        return new PagedResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber() + 1,
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
