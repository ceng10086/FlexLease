package com.flexlease.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.jwt")
public class SecurityProperties {

        private String secret = "flexlease-default-secret-please-change";
        private long accessTokenTtlSeconds = 3600;
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
