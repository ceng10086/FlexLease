package com.flexlease.payment.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.flexlease.common.dto.ApiResponse;
import com.flexlease.common.exception.BusinessException;
import com.flexlease.common.exception.ErrorCode;
import com.flexlease.common.security.JwtAuthProperties;
import com.flexlease.common.user.CreditTier;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

@Component
public class OrderServiceClient {

    private static final Logger LOG = LoggerFactory.getLogger(OrderServiceClient.class);
    private static final ParameterizedTypeReference<ApiResponse<Object>> RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<ApiResponse<OrderCreditSnapshot>> CREDIT_RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String internalToken;

    public OrderServiceClient(RestTemplate restTemplate,
                              OrderServiceProperties properties,
                              JwtAuthProperties jwtAuthProperties) {
        this.restTemplate = restTemplate;
        this.baseUrl = properties.getBaseUrl();
        this.internalToken = jwtAuthProperties.getInternalAccessToken();
    }

    public void notifyPaymentSucceeded(UUID orderId, UUID transactionId) {
        Map<String, Object> payload = Map.of("transactionId", transactionId);
        try {
            HttpHeaders headers = buildHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
                ResponseEntity<ApiResponse<Object>> response = restTemplate.exchange(
                    baseUrl + "/internal/orders/{orderId}/payment-success",
                    HttpMethod.POST,
                    new HttpEntity<>(payload, headers),
                    RESPONSE_TYPE,
                    orderId
            );
                ApiResponse<Object> body = response.getBody();
            if (body == null) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "订单服务响应为空");
            }
            if (body.code() != ErrorCode.SUCCESS.code()) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "订单服务处理失败: " + body.message());
            }
        } catch (RestClientResponseException ex) {
            LOG.warn("Order service rejected payment callback for order {}: {}", orderId, ex.getResponseBodyAsString());
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "订单服务回调失败: " + ex.getStatusText());
        } catch (RestClientException ex) {
            LOG.warn("Failed to call order service for payment success, order {}: {}", orderId, ex.getMessage());
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "订单服务不可用，请稍后重试");
        }
    }

    public OrderCreditSnapshot loadOrderCreditSnapshot(UUID orderId) {
        try {
            HttpHeaders headers = buildHeaders();
            ResponseEntity<ApiResponse<OrderCreditSnapshot>> response = restTemplate.exchange(
                    baseUrl + "/orders/{orderId}",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    CREDIT_RESPONSE_TYPE,
                    orderId
            );
            ApiResponse<OrderCreditSnapshot> body = response.getBody();
            if (body == null) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "订单服务响应为空");
            }
            if (body.code() != ErrorCode.SUCCESS.code()) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "订单服务处理失败: " + body.message());
            }
            return body.data();
        } catch (RestClientResponseException ex) {
            LOG.warn("Order service rejected credit snapshot request for order {}: {}", orderId, ex.getResponseBodyAsString());
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "订单服务查询失败: " + ex.getStatusText());
        } catch (RestClientException ex) {
            LOG.warn("Failed to call order service for credit snapshot, order {}: {}", orderId, ex.getMessage());
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "订单服务不可用，请稍后重试");
        }
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        if (internalToken != null && !internalToken.isBlank()) {
            headers.set("X-Internal-Token", internalToken);
        }
        return headers;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OrderCreditSnapshot(UUID id, Integer creditScore, CreditTier creditTier) {
    }
}
