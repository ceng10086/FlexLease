package com.flexlease.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 相关配置项（见 `application.yml` 的 `security.jwt.*`）。
 *
 * <p>约定：setter 对空值做保护，避免环境变量未设置时把默认值覆盖成空串。</p>
 */
@ConfigurationProperties(prefix = "security.jwt")
public class SecurityProperties {

        private String secret = "flexlease-default-secret-please-change";
        private long accessTokenTtlSeconds = 3600;
        private long refreshTokenTtlSeconds = 86400;
        private String issuer = "flexlease-auth-service";
        private String internalAccessToken = "flexlease-internal-secret";

        public String getSecret() {
                return secret;
        }

        public void setSecret(String secret) {
                if (secret != null && !secret.isBlank()) {
                        this.secret = secret;
                }
        }

        public long getAccessTokenTtlSeconds() {
                return accessTokenTtlSeconds;
        }

        public void setAccessTokenTtlSeconds(long accessTokenTtlSeconds) {
                if (accessTokenTtlSeconds > 0) {
                        this.accessTokenTtlSeconds = accessTokenTtlSeconds;
                }
        }

        public long getRefreshTokenTtlSeconds() {
                return refreshTokenTtlSeconds;
        }

        public void setRefreshTokenTtlSeconds(long refreshTokenTtlSeconds) {
                if (refreshTokenTtlSeconds > 0) {
                        this.refreshTokenTtlSeconds = refreshTokenTtlSeconds;
                }
        }

        public String getIssuer() {
                return issuer;
        }

        public void setIssuer(String issuer) {
                if (issuer != null && !issuer.isBlank()) {
                        this.issuer = issuer;
                }
        }

        public String getInternalAccessToken() {
                return internalAccessToken;
        }

        public void setInternalAccessToken(String internalAccessToken) {
                if (internalAccessToken != null && !internalAccessToken.isBlank()) {
                        this.internalAccessToken = internalAccessToken;
                }
        }
}
