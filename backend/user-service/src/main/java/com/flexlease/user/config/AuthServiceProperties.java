package com.flexlease.user.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 认证服务（auth-service）内部调用配置。
 * <p>
 * user-service 会在厂商入驻审核通过/冻结账号等场景调用认证中心内部接口；
 * 调用时需要在请求头携带 {@code X-Internal-Token}。
 */
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
