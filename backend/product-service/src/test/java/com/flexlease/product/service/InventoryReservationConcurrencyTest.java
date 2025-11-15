package com.flexlease.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import com.flexlease.product.domain.InventoryChangeType;
import com.flexlease.product.domain.Product;
import com.flexlease.product.domain.ProductSku;
import com.flexlease.product.domain.RentalPlan;
import com.flexlease.product.domain.RentalPlanType;
import com.flexlease.product.dto.InventoryReservationBatchRequest;
import com.flexlease.product.dto.InventoryReservationItemRequest;
import com.flexlease.product.repository.InventorySnapshotRepository;
import com.flexlease.product.repository.ProductRepository;
import com.flexlease.product.repository.ProductSkuRepository;
import com.flexlease.product.repository.RentalPlanRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:flexlease-product-concurrency;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;INIT=CREATE SCHEMA IF NOT EXISTS product",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
@SpringBootTest
class InventoryReservationConcurrencyTest {

    private static final int THREADS = 32;
    private static final int ITERATIONS_PER_THREAD = 20;
    private static final int REQUEST_QUANTITY = 1;

    @Autowired
    private InventoryReservationService reservationService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private RentalPlanRepository rentalPlanRepository;

    @Autowired
    private ProductSkuRepository productSkuRepository;

    @Autowired
    private InventorySnapshotRepository inventorySnapshotRepository;

    private UUID skuId;
    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        inventorySnapshotRepository.deleteAll();
        productSkuRepository.deleteAll();
        rentalPlanRepository.deleteAll();
        productRepository.deleteAll();

        Product product = Product.create(UUID.randomUUID(), "高并发测试商品", "TEST", "inventory stress test", null);
        productRepository.save(product);

        RentalPlan plan = RentalPlan.create(
                product,
                RentalPlanType.STANDARD,
                12,
                BigDecimal.ZERO,
                BigDecimal.ONE,
                BigDecimal.TEN,
                true,
                "MONTH",
                BigDecimal.ONE
        );
        plan.activate();
        rentalPlanRepository.save(plan);

        int initialStock = THREADS * ITERATIONS_PER_THREAD * REQUEST_QUANTITY;
        ProductSku sku = ProductSku.create(product, plan, "CONC-SKU", "{\"color\":\"black\"}", initialStock);
        productSkuRepository.save(sku);
        this.skuId = sku.getId();
        this.executor = Executors.newFixedThreadPool(THREADS);
    }

    @AfterEach
    void tearDown() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    @Test
    void processesHundredsOfConcurrentReservationsWithoutLoss() throws InterruptedException {
        CountDownLatch ready = new CountDownLatch(THREADS);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(THREADS);
        AtomicInteger failures = new AtomicInteger();

        for (int i = 0; i < THREADS; i++) {
            executor.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    for (int j = 0; j < ITERATIONS_PER_THREAD; j++) {
                        InventoryReservationBatchRequest request = new InventoryReservationBatchRequest(
                                UUID.randomUUID(),
                                List.of(new InventoryReservationItemRequest(skuId, REQUEST_QUANTITY, InventoryChangeType.RESERVE))
                        );
                        reservationService.processReservations(request);
                    }
                } catch (Exception ex) {
                    failures.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }

        ready.await(10, TimeUnit.SECONDS);
        start.countDown();
        if (!done.await(60, TimeUnit.SECONDS)) {
            fail("high concurrency test timed out");
        }

        assertThat(failures.get()).isZero();
        ProductSku refreshed = productSkuRepository.findById(skuId).orElseThrow();
        assertThat(refreshed.getStockAvailable()).isZero();
        assertThat(inventorySnapshotRepository.count())
                .isEqualTo(THREADS * (long) ITERATIONS_PER_THREAD);
    }
}
