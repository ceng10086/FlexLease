package com.flexlease.order.client;

import com.flexlease.common.dto.ApiResponse;
import com.flexlease.common.exception.BusinessException;
import com.flexlease.common.exception.ErrorCode;
import java.util.UUID;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestTemplate;

@Component
public class PaymentClient {

    private static final ParameterizedTypeReference<ApiResponse<PaymentTransactionView>> PAYMENT_RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final RestTemplate restTemplate;

    public PaymentClient(RestTemplateBuilder restTemplateBuilder, PaymentServiceProperties properties) {
        this.restTemplate = restTemplateBuilder.rootUri(properties.getBaseUrl()).build();
    }

    public PaymentTransactionView loadTransaction(UUID transactionId) {
        try {
            ResponseEntity<ApiResponse<PaymentTransactionView>> response = restTemplate.exchange(
                    "/payments/{transactionId}",
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
}
