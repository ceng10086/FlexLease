package com.flexlease.order.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.flexlease.common.user.CreditTier;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RentalOrderOrderNoTest {

    @Test
    void generatesCompactOrderNo() {
        RentalOrder order = RentalOrder.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "STANDARD",
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(10),
                BigDecimal.ZERO,
                BigDecimal.valueOf(110),
                600,
                CreditTier.STANDARD,
                BigDecimal.ONE,
                false,
                OffsetDateTime.now(),
                OffsetDateTime.now().plusDays(7)
        );

        assertThat(order.getOrderNo())
                .matches("[0-9A-Z]{14}");
    }

    @Test
    void generatesDifferentOrderNosInSmallSample() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 200; i++) {
            RentalOrder order = RentalOrder.create(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    "STANDARD",
                    BigDecimal.valueOf(100),
                    BigDecimal.valueOf(100),
                    BigDecimal.valueOf(10),
                    BigDecimal.ZERO,
                    BigDecimal.valueOf(110),
                    600,
                    CreditTier.STANDARD,
                    BigDecimal.ONE,
                    false,
                    OffsetDateTime.now(),
                    OffsetDateTime.now().plusDays(7)
            );
            seen.add(order.getOrderNo());
        }
        assertThat(seen).hasSize(200);
    }
}

