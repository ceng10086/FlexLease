package com.flexlease.notification.repository;

import com.flexlease.common.notification.NotificationChannel;
import com.flexlease.notification.domain.NotificationLog;
import com.flexlease.notification.domain.NotificationStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, UUID> {

        @Query("""
                        select log from NotificationLog log
                        where (:recipient is null or log.recipient = :recipient)
                            and (:status is null or log.status = :status)
                            and (:contextType is null or log.contextType = :contextType)
                            and (:channel is null or log.channel = :channel)
                        order by log.createdAt desc
                        """)
        List<NotificationLog> findLatest(@Param("recipient") String recipient,
                                                                         @Param("status") NotificationStatus status,
                                                                         @Param("contextType") String contextType,
                                                                         @Param("channel") NotificationChannel channel,
                                                                         Pageable pageable);
}
