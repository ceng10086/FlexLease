package com.flexlease.common.audit;

import com.flexlease.common.security.FlexleasePrincipal;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 审计日志写入器（HTTP 请求级）。
 *
 * <p>该类负责把一次 HTTP 调用的关键信息落库到 {@code audit.api_audit_log}。为了不影响主流程，
 * 写入失败会被吞掉并仅输出 debug 日志。</p>
 */
@Component
public class AuditLogWriter {

    private static final Logger LOG = LoggerFactory.getLogger(AuditLogWriter.class);

    private final JdbcTemplate jdbcTemplate;
    private final String serviceName;

    public AuditLogWriter(JdbcTemplate jdbcTemplate,
                          @Value("${spring.application.name:unknown}") String serviceName) {
        this.jdbcTemplate = jdbcTemplate;
        this.serviceName = serviceName;
    }

    public void writeHttpAudit(String method,
                               String path,
                               String queryString,
                               int statusCode,
                               long durationMs,
                               FlexleasePrincipal principal,
                               String ip,
                               String userAgent) {
        UUID userId = principal == null ? null : principal.userId();
        UUID vendorId = principal == null ? null : principal.vendorId();
        String username = principal == null ? null : principal.username();
        String roles = principal == null || principal.roles() == null || principal.roles().isEmpty()
                ? null
                : String.join(",", principal.roles());

        try {
            jdbcTemplate.update("""
                            INSERT INTO audit.api_audit_log (
                              service_name,
                              method,
                              path,
                              query_string,
                              status_code,
                              duration_ms,
                              principal_user_id,
                              principal_vendor_id,
                              principal_username,
                              roles,
                              ip,
                              user_agent
                            ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)
                            """,
                    serviceName,
                    truncate(method, 10),
                    truncate(path, 500),
                    truncate(queryString, 2000),
                    statusCode,
                    durationMs,
                    userId,
                    vendorId,
                    truncate(username, 100),
                    truncate(roles, 500),
                    truncate(ip, 64),
                    truncate(userAgent, 1000)
            );
        } catch (Exception ex) {
            LOG.debug("Skip persisting audit log due to error: {}", ex.getMessage());
        }
    }

    private String truncate(String value, int maxLen) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() <= maxLen) {
            return trimmed;
        }
        return trimmed.substring(0, maxLen);
    }
}
