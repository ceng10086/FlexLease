package com.flexlease.common.notification;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;

/**
 * 通知发送请求（跨服务复用的 DTO）。
 *
 * <p>用于调用 notification-service 的发送接口：可指定 {@code templateCode} 走模板渲染，
 * 或直接传入 {@code subject/content} 走自定义内容。</p>
 */
public record NotificationSendRequest(
        String templateCode,
        @NotBlank @Size(max = 100) String recipient,
        @Size(max = 200) String subject,
        String content,
        Map<String, Object> variables,
        @Size(max = 50) String contextType,
        @Size(max = 100) String contextReference
) {

    public NotificationSendRequest(String templateCode,
                                    @NotBlank @Size(max = 100) String recipient,
                                    @Size(max = 200) String subject,
                                    String content,
                                    Map<String, Object> variables) {
        this(templateCode, recipient, subject, content, variables, null, null);
    }

    /**
     * 是否使用模板发送。
     */
    public boolean hasTemplate() {
        return templateCode != null && !templateCode.isBlank();
    }
}
