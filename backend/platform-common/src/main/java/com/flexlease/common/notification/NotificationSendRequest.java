package com.flexlease.common.notification;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;

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

    public boolean hasTemplate() {
        return templateCode != null && !templateCode.isBlank();
    }
}
