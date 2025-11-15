package com.flexlease.product.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "flexlease.inventory")
public class InventoryConcurrencyProperties {

    private final Concurrency concurrency = new Concurrency();

    public Concurrency getConcurrency() {
        return concurrency;
    }

    public static class Concurrency {
        /**
         * Maximum retry attempts when optimistic locking conflicts occur.
         */
        private int maxAttempts = 40;

        /**
         * Base backoff duration applied between retries. The wait time grows linearly with the attempt count.
         */
        private Duration backoff = Duration.ofMillis(2);

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public Duration getBackoff() {
            return backoff;
        }

        public void setBackoff(Duration backoff) {
            this.backoff = backoff;
        }
    }
}
