package com.flexlease.common.audit;

import com.flexlease.common.security.FlexleasePrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class HttpAuditLogFilter extends OncePerRequestFilter {

    private static final String PRINCIPAL_REQUEST_ATTRIBUTE = "flexlease.principal";

    private final AuditLogWriter auditLogWriter;
    private final boolean enabled;
    private final List<String> ignorePathPatterns;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public HttpAuditLogFilter(AuditLogWriter auditLogWriter,
                              @Value("${flexlease.audit.enabled:true}") boolean enabled,
                              @Value("${flexlease.audit.ignore-paths:/actuator/**,/error}") String ignorePaths) {
        this.auditLogWriter = auditLogWriter;
        this.enabled = enabled;
        this.ignorePathPatterns = Arrays.stream(ignorePaths.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!enabled) {
            return true;
        }
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        return ignorePathPatterns.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        StatusCaptureResponseWrapper responseWrapper = new StatusCaptureResponseWrapper(response);
        long start = System.nanoTime();
        try {
            filterChain.doFilter(request, responseWrapper);
        } catch (RuntimeException ex) {
            responseWrapper.markUnhandledException();
            throw ex;
        } finally {
            long durationMs = Math.max(0, (System.nanoTime() - start) / 1_000_000);
            int statusCode = responseWrapper.getStatus();
            if (responseWrapper.hasUnhandledException() && statusCode < 500) {
                statusCode = 500;
            }
            FlexleasePrincipal principal = null;
            Object principalAttr = request.getAttribute(PRINCIPAL_REQUEST_ATTRIBUTE);
            if (principalAttr instanceof FlexleasePrincipal flexleasePrincipal) {
                principal = flexleasePrincipal;
            }
            auditLogWriter.writeHttpAudit(
                    request.getMethod(),
                    request.getRequestURI(),
                    request.getQueryString(),
                    statusCode,
                    durationMs,
                    principal,
                    resolveClientIp(request),
                    request.getHeader("User-Agent")
            );
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            int commaIndex = forwardedFor.indexOf(',');
            if (commaIndex > 0) {
                return forwardedFor.substring(0, commaIndex).trim();
            }
            return forwardedFor.trim();
        }
        return request.getRemoteAddr();
    }

    private static final class StatusCaptureResponseWrapper extends HttpServletResponseWrapper {
        private int httpStatus = HttpServletResponse.SC_OK;
        private boolean unhandledException;

        private StatusCaptureResponseWrapper(HttpServletResponse response) {
            super(response);
        }

        @Override
        public void setStatus(int sc) {
            this.httpStatus = sc;
            super.setStatus(sc);
        }

        @Override
        public void sendError(int sc) throws IOException {
            this.httpStatus = sc;
            super.sendError(sc);
        }

        @Override
        public void sendError(int sc, String msg) throws IOException {
            this.httpStatus = sc;
            super.sendError(sc, msg);
        }

        @Override
        public void sendRedirect(String location) throws IOException {
            this.httpStatus = HttpServletResponse.SC_FOUND;
            super.sendRedirect(location);
        }

        @Override
        public int getStatus() {
            return httpStatus;
        }

        private void markUnhandledException() {
            this.unhandledException = true;
        }

        private boolean hasUnhandledException() {
            return unhandledException;
        }
    }
}
