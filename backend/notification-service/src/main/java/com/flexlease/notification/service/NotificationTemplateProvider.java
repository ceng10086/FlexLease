package com.flexlease.notification.service;

import com.flexlease.notification.domain.NotificationTemplate;
import com.flexlease.notification.repository.NotificationTemplateRepository;
import java.util.Optional;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

/**
 * 通知模板读取器（带缓存）。
 * <p>
 * 站内信模板相对稳定，使用 Redis 缓存可减少 DB 读取；模板变更后可通过 {@link #evict(String)} 清理缓存。
 */
@Component
public class NotificationTemplateProvider {

    private static final String CACHE_NAME = "notification:templates";

    private final NotificationTemplateRepository templateRepository;

    public NotificationTemplateProvider(NotificationTemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    @Cacheable(cacheNames = CACHE_NAME, key = "#code")
    public Optional<NotificationTemplate> findByCode(String code) {
        return templateRepository.findByCode(code);
    }

    @CacheEvict(cacheNames = CACHE_NAME, key = "#code")
    public void evict(String code) {
        // 模板更新后用于清理缓存（避免读到旧模板）
    }
}
