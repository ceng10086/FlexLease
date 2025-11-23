package com.flexlease.order.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.flexlease.common.dto.ApiResponse;
import com.flexlease.common.exception.BusinessException;
import com.flexlease.common.exception.ErrorCode;
import com.flexlease.common.security.JwtAuthProperties;
import com.flexlease.common.user.CreditTier;
import java.util.Map;
import java.util.UUID;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class UserProfileClient {

    private static final ParameterizedTypeReference<ApiResponse<UserCreditView>> RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private static final ParameterizedTypeReference<ApiResponse<Map<String, Object>>> ADJUST_RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String internalToken;

    public UserProfileClient(RestTemplate restTemplate,
                             UserServiceProperties properties,
                             JwtAuthProperties jwtAuthProperties) {
        this.restTemplate = restTemplate;
        this.baseUrl = properties.getBaseUrl();
        this.internalToken = jwtAuthProperties.getInternalAccessToken();
    }

    public UserCreditView loadCredit(UUID userId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            if (internalToken != null && !internalToken.isBlank()) {
                headers.set("X-Internal-Token", internalToken);
            }
            ResponseEntity<ApiResponse<UserCreditView>> response = restTemplate.exchange(
                    baseUrl + "/internal/users/{userId}/credit",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    RESPONSE_TYPE,
                    userId
            );
            ApiResponse<UserCreditView> body = response.getBody();
            if (body == null || body.data() == null) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "用户服务返回为空");
            }
            if (body.code() != ErrorCode.SUCCESS.code()) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "用户服务返回异常: " + body.message());
            }
            return body.data();
        } catch (HttpClientErrorException.NotFound ex) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "用户不存在或未创建档案");
        } catch (RestClientException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "用户服务调用失败");
        }
    }

    public void adjustCredit(UUID userId, int delta, String reason) {
        try {
            HttpHeaders headers = new HttpHeaders();
            if (internalToken != null && !internalToken.isBlank()) {
                headers.set("X-Internal-Token", internalToken);
            }
            CreditAdjustmentPayload payload = new CreditAdjustmentPayload(delta, reason);
            ResponseEntity<ApiResponse<Map<String, Object>>> response = restTemplate.exchange(
                    baseUrl + "/internal/users/{userId}/credit-adjustments",
                    HttpMethod.POST,
                    new HttpEntity<>(payload, headers),
                    ADJUST_RESPONSE_TYPE,
                    userId
            );
            ApiResponse<Map<String, Object>> body = response.getBody();
            if (body != null && body.code() != ErrorCode.SUCCESS.code()) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "用户服务返回异常: " + body.message());
            }
        } catch (RestClientException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "用户服务调用失败");
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UserCreditView(UUID userId, Integer creditScore, CreditTier creditTier) {
    }

    private record CreditAdjustmentPayload(Integer delta, String reason) {
    }
}
