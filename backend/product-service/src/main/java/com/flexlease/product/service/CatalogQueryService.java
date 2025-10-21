package com.flexlease.product.service;

import com.flexlease.common.exception.BusinessException;
import com.flexlease.common.exception.ErrorCode;
import com.flexlease.product.domain.Product;
import com.flexlease.product.domain.ProductStatus;
import com.flexlease.product.dto.CatalogProductResponse;
import com.flexlease.product.dto.PagedResponse;
import com.flexlease.product.repository.ProductRepository;
import jakarta.transaction.Transactional;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@Transactional(Transactional.TxType.SUPPORTS)
public class CatalogQueryService {

    private final ProductRepository productRepository;
    private final ProductAssembler assembler;

    public CatalogQueryService(ProductRepository productRepository, ProductAssembler assembler) {
        this.productRepository = productRepository;
        this.assembler = assembler;
    }

    public PagedResponse<CatalogProductResponse> listActive(String categoryCode, String keyword, Pageable pageable) {
        Page<Product> page;
        boolean hasCategory = StringUtils.hasText(categoryCode);
        boolean hasKeyword = StringUtils.hasText(keyword);
        if (hasCategory && hasKeyword) {
            page = productRepository.findByStatusAndCategoryCodeAndNameContainingIgnoreCase(ProductStatus.ACTIVE, categoryCode, keyword, pageable);
        } else if (hasCategory) {
            page = productRepository.findByStatusAndCategoryCode(ProductStatus.ACTIVE, categoryCode, pageable);
        } else if (hasKeyword) {
            page = productRepository.findByStatusAndNameContainingIgnoreCase(ProductStatus.ACTIVE, keyword, pageable);
        } else {
            page = productRepository.findByStatus(ProductStatus.ACTIVE, pageable);
        }
        return toPagedResponse(page, assembler::toCatalog);
    }

    public CatalogProductResponse getProduct(UUID productId) {
        Product product = productRepository.findWithPlansById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "商品不存在"));
        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "商品未上架");
        }
        return assembler.toCatalog(product);
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
