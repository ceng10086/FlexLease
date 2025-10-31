package com.flexlease.user.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "flexlease.auth-service")
public class AuthServiceProperties {

    private String baseUrl = "http://auth-service";
    private String internalToken = "flexlease-internal-secret";

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        if (baseUrl != null && !baseUrl.isBlank()) {
            this.baseUrl = baseUrl;
        }
    }

    public String getInternalToken() {
        return internalToken;
    }

    public void setInternalToken(String internalToken) {
        if (internalToken != null && !internalToken.isBlank()) {
            this.internalToken = internalToken;
        }
    }
}
