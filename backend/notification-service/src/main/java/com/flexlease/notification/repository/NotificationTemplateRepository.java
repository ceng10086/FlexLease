package com.flexlease.notification.repository;

import com.flexlease.notification.domain.NotificationTemplate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, java.util.UUID> {

    Optional<NotificationTemplate> findByCode(String code);
}
