package com.flexlease.order.client;

import com.flexlease.common.dto.ApiResponse;
import com.flexlease.common.exception.BusinessException;
import com.flexlease.common.exception.ErrorCode;
import com.flexlease.common.security.JwtAuthProperties;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * 支付服务客户端：查询流水并触发内部退款。
 * <p>
 * 所有调用均以内部令牌互信方式访问（{@code X-Internal-Token}），用于订单状态流转与押金退款等场景。
 */
@Component
public class PaymentClient {

    private static final ParameterizedTypeReference<ApiResponse<PaymentTransactionView>> PAYMENT_RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private static final ParameterizedTypeReference<ApiResponse<RefundTransactionRecord>> REFUND_RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String internalToken;

    public PaymentClient(RestTemplate restTemplate, PaymentServiceProperties properties, JwtAuthProperties jwtAuthProperties) {
        this.restTemplate = restTemplate;
        this.baseUrl = properties.getBaseUrl();
        this.internalToken = jwtAuthProperties.getInternalAccessToken();
    }

    public PaymentTransactionView loadTransaction(UUID transactionId) {
        try {
            HttpHeaders headers = buildInternalHeaders();
            ResponseEntity<ApiResponse<PaymentTransactionView>> response = restTemplate.exchange(
                    baseUrl + "/payments/{transactionId}",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    PAYMENT_RESPONSE_TYPE,
                    transactionId
            );
            ApiResponse<PaymentTransactionView> body = response.getBody();
            if (body == null || body.data() == null) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "支付服务返回为空");
            }
            if (body.code() != ErrorCode.SUCCESS.code()) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "支付服务处理失败: " + body.message());
            }
            return body.data();
        } catch (HttpClientErrorException.NotFound ex) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "支付流水不存在");
        } catch (RestClientException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "支付服务调用失败");
        }
    }

    public RefundTransactionRecord createRefund(UUID transactionId, BigDecimal amount, String reason) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("amount", amount);
        if (reason != null && !reason.isBlank()) {
            payload.put("reason", reason);
        }
        try {
            HttpHeaders headers = buildInternalHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            ResponseEntity<ApiResponse<RefundTransactionRecord>> response = restTemplate.exchange(
                    baseUrl + "/internal/payments/{transactionId}/refund",
                    HttpMethod.POST,
                    new HttpEntity<>(payload, headers),
                    REFUND_RESPONSE_TYPE,
                    transactionId
            );
            ApiResponse<RefundTransactionRecord> body = response.getBody();
            if (body == null) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "支付退款返回为空");
            }
            if (body.code() != ErrorCode.SUCCESS.code()) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "支付退款失败: " + body.message());
            }
            return body.data();
        } catch (HttpClientErrorException.NotFound ex) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "支付流水不存在");
        } catch (RestClientException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "支付服务调用失败");
        }
    }

    public record RefundTransactionRecord(UUID id, BigDecimal amount) {
    }

    private HttpHeaders buildInternalHeaders() {
        HttpHeaders headers = new HttpHeaders();
        if (internalToken != null && !internalToken.isBlank()) {
            headers.set("X-Internal-Token", internalToken);
        }
        return headers;
    }
}
