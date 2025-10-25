package com.flexlease.notification.repository;

import com.flexlease.notification.domain.NotificationLog;
import com.flexlease.notification.domain.NotificationStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, UUID> {

    List<NotificationLog> findTop50ByStatusOrderByCreatedAtDesc(NotificationStatus status);

    List<NotificationLog> findTop50ByOrderByCreatedAtDesc();
}
