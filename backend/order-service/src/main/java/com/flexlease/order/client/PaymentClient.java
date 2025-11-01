package com.flexlease.order.client;

import com.flexlease.common.dto.ApiResponse;
import com.flexlease.common.exception.BusinessException;
import com.flexlease.common.exception.ErrorCode;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

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

    public PaymentClient(RestTemplate restTemplate, PaymentServiceProperties properties) {
        this.restTemplate = restTemplate;
        this.baseUrl = properties.getBaseUrl();
    }

    public PaymentTransactionView loadTransaction(UUID transactionId) {
        try {
            ResponseEntity<ApiResponse<PaymentTransactionView>> response = restTemplate.exchange(
                    baseUrl + "/payments/{transactionId}",
                    HttpMethod.GET,
                    null,
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
            ResponseEntity<ApiResponse<RefundTransactionRecord>> response = restTemplate.exchange(
                    baseUrl + "/payments/{transactionId}/refund",
                    HttpMethod.POST,
                    new HttpEntity<>(payload),
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
}
