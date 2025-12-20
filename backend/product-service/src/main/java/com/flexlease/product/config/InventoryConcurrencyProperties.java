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
         * 乐观锁冲突时的最大重试次数。
         */
        private int maxAttempts = 40;

        /**
         * 每次重试之间的基础退避时间；等待时长会随着重试次数线性增长。
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
