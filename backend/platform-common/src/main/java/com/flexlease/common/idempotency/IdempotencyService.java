package com.flexlease.common.idempotency;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

/**
 * Very lightweight in-memory idempotency helper that keeps the first successful
 * result for a given key within a small time window and replays it for repeated
 * requests. The implementation is intentionally simple to satisfy the project
 * requirements without introducing external storage.
 */
@Component
public class IdempotencyService {

    private final ConcurrentMap<String, CacheEntry> storage = new ConcurrentHashMap<>();

    /**
     * Executes the supplied action once per key within the provided TTL. When the
     * same key is seen again before the TTL elapses, the cached result is returned
     * instead of invoking the action.
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
