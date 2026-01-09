package com.flexlease.product.service;

import com.flexlease.common.exception.BusinessException;
import com.flexlease.common.exception.ErrorCode;
import com.flexlease.product.domain.Product;
import com.flexlease.product.domain.ProductStatus;
import com.flexlease.product.domain.RentalPlan;
import com.flexlease.product.domain.RentalPlanType;
import com.flexlease.product.dto.CatalogProductResponse;
import com.flexlease.product.dto.PagedResponse;
import com.flexlease.product.repository.ProductRepository;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@Transactional(Transactional.TxType.SUPPORTS)
public class CatalogQueryService {

    /**
     * Catalog 查询服务（面向消费者）。
     * <p>
     * 为了保持实现简单：当使用“高级筛选”（按租赁类型/押金区间/租金排序）时，会先查询出满足基础条件的全部商品，
     * 再在内存中进行过滤与排序，最后手动分页；基础查询则直接走数据库分页。
     */
    private final ProductRepository productRepository;
    private final ProductAssembler assembler;

    public CatalogQueryService(ProductRepository productRepository, ProductAssembler assembler) {
        this.productRepository = productRepository;
        this.assembler = assembler;
    }

    public PagedResponse<CatalogProductResponse> listActive(String categoryCode,
                                                            String keyword,
                                                            RentalPlanType planType,
                                                            BigDecimal minDeposit,
                                                            BigDecimal maxDeposit,
                                                            String rentSort,
                                                            Pageable pageable) {
        boolean advancedFilters = planType != null || minDeposit != null || maxDeposit != null || StringUtils.hasText(rentSort);
        if (!advancedFilters) {
            return listActiveBasic(categoryCode, keyword, pageable);
        }
        List<Product> products = listActiveUnpaged(categoryCode, keyword);
        List<Product> filtered = products.stream()
                .filter(product -> matchesPlanAndDeposit(product, planType, minDeposit, maxDeposit))
                .sorted(buildSortComparator(planType, rentSort))
                .toList();
        return toPagedResponse(filtered, pageable, assembler::toCatalog);
    }

    private PagedResponse<CatalogProductResponse> listActiveBasic(String categoryCode, String keyword, Pageable pageable) {
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

    private List<Product> listActiveUnpaged(String categoryCode, String keyword) {
        Page<Product> page;
        boolean hasCategory = StringUtils.hasText(categoryCode);
        boolean hasKeyword = StringUtils.hasText(keyword);
        if (hasCategory && hasKeyword) {
            page = productRepository.findByStatusAndCategoryCodeAndNameContainingIgnoreCase(ProductStatus.ACTIVE, categoryCode, keyword, Pageable.unpaged());
        } else if (hasCategory) {
            page = productRepository.findByStatusAndCategoryCode(ProductStatus.ACTIVE, categoryCode, Pageable.unpaged());
        } else if (hasKeyword) {
            page = productRepository.findByStatusAndNameContainingIgnoreCase(ProductStatus.ACTIVE, keyword, Pageable.unpaged());
        } else {
            page = productRepository.findByStatus(ProductStatus.ACTIVE, Pageable.unpaged());
        }
        return page.getContent();
    }

    private boolean matchesPlanAndDeposit(Product product,
                                         RentalPlanType planType,
                                         BigDecimal minDeposit,
                                         BigDecimal maxDeposit) {
        var plans = product.getRentalPlans();
        if (plans == null || plans.isEmpty()) {
            return false;
        }
        List<RentalPlan> candidatePlans = plans.stream()
                .filter(plan -> planType == null || plan.getPlanType() == planType)
                .toList();
        if (candidatePlans.isEmpty()) {
            return false;
        }
        if (minDeposit == null && maxDeposit == null) {
            return true;
        }
        return candidatePlans.stream().anyMatch(plan -> within(plan.getDepositAmount(), minDeposit, maxDeposit));
    }

    private boolean within(BigDecimal value, BigDecimal min, BigDecimal max) {
        if (value == null) {
            return false;
        }
        if (min != null && value.compareTo(min) < 0) {
            return false;
        }
        if (max != null && value.compareTo(max) > 0) {
            return false;
        }
        return true;
    }

    private Comparator<Product> buildSortComparator(RentalPlanType planType, String rentSort) {
        Comparator<Product> fallback = Comparator.comparing(Product::getCreatedAt).reversed();
        if (!StringUtils.hasText(rentSort)) {
            return fallback;
        }
        Comparator<Product> rentComparator = Comparator.comparing(product -> minRent(product, planType), Comparator.nullsLast(BigDecimal::compareTo));
        if ("RENT_DESC".equalsIgnoreCase(rentSort)) {
            rentComparator = rentComparator.reversed();
        } else if (!"RENT_ASC".equalsIgnoreCase(rentSort)) {
            return fallback;
        }
        return rentComparator.thenComparing(fallback);
    }

    private BigDecimal minRent(Product product, RentalPlanType planType) {
        if (product.getRentalPlans() == null) {
            return null;
        }
        return product.getRentalPlans().stream()
                .filter(plan -> planType == null || plan.getPlanType() == planType)
                .map(RentalPlan::getRentAmountMonthly)
                .filter(value -> value != null)
                .min(BigDecimal::compareTo)
                .orElse(null);
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

    private <T, R> PagedResponse<R> toPagedResponse(List<T> items, Pageable pageable, Function<T, R> mapper) {
        int page = Math.max(pageable.getPageNumber(), 0);
        int size = Math.max(pageable.getPageSize(), 1);
        int totalElements = items.size();
        int from = page * size;
        if (from >= totalElements) {
            int totalPages = (int) Math.ceil(totalElements / (double) size);
            return new PagedResponse<>(List.of(), page + 1, size, totalElements, totalPages);
        }
        int to = Math.min(from + size, totalElements);
        List<R> content = items.subList(from, to).stream().map(mapper).toList();
        int totalPages = (int) Math.ceil(totalElements / (double) size);
        return new PagedResponse<>(content, page + 1, size, totalElements, totalPages);
    }
}
