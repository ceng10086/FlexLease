package com.flexlease.payment.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.flexlease.common.dto.ApiResponse;
import com.flexlease.common.exception.BusinessException;
import com.flexlease.common.exception.ErrorCode;
import com.flexlease.common.security.JwtAuthProperties;
import java.math.BigDecimal;
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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * user-service（厂商域）调用封装。
 *
 * <p>读取厂商抽成档案（行业/信用档/SLA 等），用于计算平台抽成比例并写入支付流水快照。</p>
 */
@Component
public class VendorServiceClient {

    private static final Logger LOG = LoggerFactory.getLogger(VendorServiceClient.class);
    private static final ParameterizedTypeReference<ApiResponse<VendorCommissionProfile>> RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String internalToken;

    public VendorServiceClient(RestTemplate restTemplate,
                               UserServiceProperties properties,
                               JwtAuthProperties jwtAuthProperties) {
        this.restTemplate = restTemplate;
        this.baseUrl = properties.getBaseUrl();
        this.internalToken = jwtAuthProperties.getInternalAccessToken();
    }

    public VendorCommissionProfile loadCommissionProfile(UUID vendorId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (internalToken != null && !internalToken.isBlank()) {
                headers.set("X-Internal-Token", internalToken);
            }
            ResponseEntity<ApiResponse<VendorCommissionProfile>> response = restTemplate.exchange(
                    baseUrl + "/internal/vendors/{vendorId}/commission-profile",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    RESPONSE_TYPE,
                    vendorId
            );
            ApiResponse<VendorCommissionProfile> body = response.getBody();
            if (body == null || body.data() == null) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "用户服务返回为空");
            }
            if (body.code() != ErrorCode.SUCCESS.code()) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "用户服务返回异常: " + body.message());
            }
            return body.data();
        } catch (HttpClientErrorException.NotFound ex) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "厂商不存在");
        } catch (RestClientException ex) {
            LOG.warn("Failed to call user-service for vendor {} commission profile: {}", vendorId, ex.getMessage());
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "查询厂商抽成信息失败");
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    /**
     * 厂商抽成档案（user-service 返回的只读结构）。
     */
    public record VendorCommissionProfile(String industryCategory,
                                          BigDecimal baseRate,
                                          String creditTier,
                                          Integer slaScore,
                                          BigDecimal commissionRate) {
    }
}
