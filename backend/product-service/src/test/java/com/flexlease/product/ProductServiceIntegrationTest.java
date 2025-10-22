package com.flexlease.product;

import static org.assertj.core.api.Assertions.assertThat;

import com.flexlease.product.domain.InventoryChangeType;
import com.flexlease.product.domain.ProductStatus;
import com.flexlease.product.domain.RentalPlanType;
import com.flexlease.product.dto.CatalogProductResponse;
import com.flexlease.product.dto.InventoryAdjustRequest;
import com.flexlease.product.dto.PagedResponse;
import com.flexlease.product.dto.ProductRequest;
import com.flexlease.product.dto.ProductResponse;
import com.flexlease.product.dto.RentalPlanRequest;
import com.flexlease.product.dto.RentalPlanResponse;
import com.flexlease.product.dto.SkuRequest;
import com.flexlease.product.dto.SkuResponse;
import com.flexlease.product.service.CatalogQueryService;
import com.flexlease.product.service.ProductAdminService;
import com.flexlease.product.service.VendorProductService;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;

@SpringBootTest
class ProductServiceIntegrationTest {

    @Autowired
    private VendorProductService vendorProductService;

    @Autowired
    private ProductAdminService productAdminService;

    @Autowired
    private CatalogQueryService catalogQueryService;

    @Test
    void endToEndProductLifecycle() {
        UUID vendorId = UUID.randomUUID();

        ProductRequest productRequest = new ProductRequest(
                "共享办公桌",
                "OFFICE",
                "高品质共享办公桌，按月租赁",
                "https://cdn.flexlease.test/images/desk.png"
        );
        ProductResponse createdProduct = vendorProductService.createProduct(vendorId, productRequest);

        RentalPlanRequest planRequest = new RentalPlanRequest(
                RentalPlanType.STANDARD,
                12,
                new BigDecimal("500.00"),
                new BigDecimal("299.00"),
                new BigDecimal("2599.00"),
                true,
                "MONTH",
                new BigDecimal("299.00")
        );
        RentalPlanResponse plan = vendorProductService.createPlan(vendorId, createdProduct.id(), planRequest);
        vendorProductService.activatePlan(vendorId, createdProduct.id(), plan.id());

        SkuRequest skuRequest = new SkuRequest(
                "DESK-001",
                Map.of("color", "black", "size", "120x60"),
                5,
                null,
                null
        );
        SkuResponse sku = vendorProductService.createSku(vendorId, createdProduct.id(), plan.id(), skuRequest);
        assertThat(sku.stockAvailable()).isEqualTo(5);

        SkuResponse afterInbound = vendorProductService.adjustInventory(vendorId, createdProduct.id(), plan.id(), sku.id(),
                new InventoryAdjustRequest(InventoryChangeType.INBOUND, 10, null));
        assertThat(afterInbound.stockTotal()).isEqualTo(15);
        assertThat(afterInbound.stockAvailable()).isEqualTo(15);

        SkuResponse afterReserve = vendorProductService.adjustInventory(vendorId, createdProduct.id(), plan.id(), sku.id(),
                new InventoryAdjustRequest(InventoryChangeType.RESERVE, 3, null));
        assertThat(afterReserve.stockAvailable()).isEqualTo(12);
        SkuResponse afterRelease = vendorProductService.adjustInventory(vendorId, createdProduct.id(), plan.id(), sku.id(),
                new InventoryAdjustRequest(InventoryChangeType.RELEASE, 3, null));
        assertThat(afterRelease.stockAvailable()).isEqualTo(15);

        ProductResponse pending = vendorProductService.submitForReview(vendorId, createdProduct.id());
        assertThat(pending.status()).isEqualTo(ProductStatus.PENDING_REVIEW);

        UUID reviewerId = UUID.randomUUID();
        ProductResponse approved = productAdminService.approveProduct(createdProduct.id(), reviewerId, "审核通过");
        assertThat(approved.status()).isEqualTo(ProductStatus.ACTIVE);
        assertThat(approved.reviewedBy()).isEqualTo(reviewerId);

        PagedResponse<CatalogProductResponse> catalogList = catalogQueryService.listActive(null, null, PageRequest.of(0, 10));
        assertThat(catalogList.content()).hasSize(1);
        CatalogProductResponse catalogProduct = catalogList.content().getFirst();
        assertThat(catalogProduct.product().status()).isEqualTo(ProductStatus.ACTIVE);
        assertThat(catalogProduct.plans()).isNotEmpty();

        CatalogProductResponse detail = catalogQueryService.getProduct(createdProduct.id());
        assertThat(detail.product().id()).isEqualTo(createdProduct.id());
    }
}
