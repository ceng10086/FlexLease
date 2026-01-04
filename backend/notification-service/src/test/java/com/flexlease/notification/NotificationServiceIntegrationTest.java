package com.flexlease.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.flexlease.common.exception.BusinessException;
import com.flexlease.common.notification.NotificationSendRequest;
import com.flexlease.common.security.FlexleasePrincipal;
import com.flexlease.notification.domain.NotificationStatus;
import com.flexlease.notification.repository.NotificationLogRepository;
import com.flexlease.notification.repository.NotificationTemplateRepository;
import com.flexlease.notification.service.NotificationService;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class NotificationServiceIntegrationTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationLogRepository notificationLogRepository;

    @Autowired
    private NotificationTemplateRepository notificationTemplateRepository;

    @BeforeEach
    void setUp() {
        assertThat(notificationTemplateRepository.findByCode("ORDER_SHIPPED")).isPresent();
    }

    @Test
    void shouldSendNotificationUsingTemplate() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("orderNo", "ORD123");
        variables.put("carrier", "SF");
        variables.put("trackingNo", "SF999999");
        UUID recipient = UUID.randomUUID();

        var response = notificationService.sendNotification(new NotificationSendRequest(
                "ORDER_SHIPPED",
                recipient.toString(),
                null,
                null,
                variables
        ));

        assertThat(response.status()).isEqualTo(NotificationStatus.SENT);
        assertThat(response.subject()).isEqualTo("订单发货提醒");
        assertThat(response.content()).contains("ORD123").contains("SF999999");
        assertThat(notificationLogRepository.findById(response.id())).isPresent();
    }

    @Test
    void shouldSendWithCustomContentWhenNoTemplateProvided() {
        var response = notificationService.sendNotification(new NotificationSendRequest(
                null,
                "user-1",
                "测试标题",
                "内容正文",
                Map.of()
        ));

        assertThat(response.status()).isEqualTo(NotificationStatus.SENT);
        assertThat(response.subject()).isEqualTo("测试标题");
        assertThat(response.content()).isEqualTo("内容正文");
    }

    @Test
    void vendorShouldSeeOwnNotificationsByDefault() {
        UUID vendorId = UUID.randomUUID();
        notificationService.sendNotification(new NotificationSendRequest(
                null,
                vendorId.toString(),
                "新订单提醒",
                "您有新的订单",
                Map.of()
        ));

        try (SecurityContextHandle ignored = withPrincipal(UUID.randomUUID(), vendorId, "vendor-user", Set.of("VENDOR"))) {
            var logs = notificationService.listLogs(null, null);
            assertThat(logs).isNotEmpty();
            assertThat(logs.getFirst().recipient()).isEqualTo(vendorId.toString());
        }
    }

    @Test
    void vendorCannotInspectOtherVendorNotifications() {
        UUID vendorId = UUID.randomUUID();
        UUID otherVendorId = UUID.randomUUID();
        notificationService.sendNotification(new NotificationSendRequest(
                null,
                vendorId.toString(),
                "订单提醒",
                "A",
                Map.of()
        ));

        try (SecurityContextHandle ignored = withPrincipal(UUID.randomUUID(), vendorId, "vendor-user", Set.of("VENDOR"))) {
            assertThatThrownBy(() -> notificationService.listLogs(null, otherVendorId.toString()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("禁止查看其他厂商的通知");
        }
    }

    @Test
    void adminCanFilterByContextType() {
        UUID disputeRecipient = UUID.randomUUID();
        UUID disputeId = UUID.randomUUID();
        notificationService.sendNotification(new NotificationSendRequest(
                null,
                disputeRecipient.toString(),
                "纠纷提醒",
                "请尽快处理纠纷",
                Map.of(),
                "DISPUTE",
                disputeId.toString()
        ));
        notificationService.sendNotification(new NotificationSendRequest(
                null,
                disputeRecipient.toString(),
                "普通提醒",
                "系统通知",
                Map.of()
        ));

        try (SecurityContextHandle ignored = withPrincipal(UUID.randomUUID(), null, "admin-user", Set.of("ADMIN"))) {
            var logs = notificationService.listLogs(null, null, "dispute");
            assertThat(logs).isNotEmpty();
            assertThat(logs).allMatch(log -> "DISPUTE".equals(log.contextType()));
        }
    }

    private SecurityContextHandle withPrincipal(UUID userId, UUID vendorId, String username, Set<String> roles) {
        FlexleasePrincipal principal = new FlexleasePrincipal(userId, vendorId, username, roles);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null));
        return new SecurityContextHandle();
    }

    private static final class SecurityContextHandle implements AutoCloseable {
        @Override
        public void close() {
            SecurityContextHolder.clearContext();
        }
    }
}
