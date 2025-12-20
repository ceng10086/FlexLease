package com.flexlease.common.idempotency;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

/**
 * 极简的内存幂等工具：在一个很短的时间窗口内缓存首次成功结果，对相同 key 的重复请求直接返回缓存结果。
 * <p>
 * 为了符合课程项目的 KISS 原则，这里不引入 Redis 等外部存储。
 */
@Component
public class IdempotencyService {

    private final ConcurrentMap<String, CacheEntry> storage = new ConcurrentHashMap<>();

    /**
     * 在给定 TTL 内，同一个 key 只执行一次 action；TTL 未过期前重复调用会直接返回缓存结果。
     */
    public <T> T execute(String key, Duration ttl, Supplier<T> action) {
        Objects.requireNonNull(key, "Idempotency key must not be null");
        Objects.requireNonNull(ttl, "TTL must not be null");
        Objects.requireNonNull(action, "Action must not be null");

        CacheEntry entry = storage.compute(key, (k, existing) -> {
            if (existing != null && !existing.isExpired(ttl)) {
                return existing;
            }
            T result = action.get();
            return new CacheEntry(result, Instant.now());
        });
        @SuppressWarnings("unchecked")
        T value = (T) entry.value();
        return value;
    }

    private record CacheEntry(Object value, Instant storedAt) {
        boolean isExpired(Duration ttl) {
            if (ttl.isZero() || ttl.isNegative()) {
                return true;
            }
            return storedAt.plus(ttl).isBefore(Instant.now());
        }
    }
}
