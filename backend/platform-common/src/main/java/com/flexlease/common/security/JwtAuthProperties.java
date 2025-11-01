package com.flexlease.common.security;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.jwt")
public class JwtAuthProperties {

    private String secret = "flexlease-default-secret-please-change";
    private String issuer = "flexlease-auth-service";
    private String internalAccessToken = "flexlease-internal-secret";
    private final List<String> permitAll = new ArrayList<>(List.of("/actuator/health", "/actuator/info"));

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        if (secret != null && !secret.isBlank()) {
            this.secret = secret;
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

    public List<String> getPermitAll() {
        return permitAll;
    }
}
