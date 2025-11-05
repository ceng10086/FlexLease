package com.flexlease.order.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.flexlease.common.dto.ApiResponse;
import com.flexlease.common.exception.BusinessException;
import com.flexlease.common.exception.ErrorCode;
import com.flexlease.common.security.JwtAuthProperties;
import java.util.List;
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
public class ProductCatalogClient {

    private static final ParameterizedTypeReference<ApiResponse<CatalogProductView>> RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String internalToken;

    public ProductCatalogClient(RestTemplate restTemplate, ProductServiceProperties properties, JwtAuthProperties jwtAuthProperties) {
        this.restTemplate = restTemplate;
        this.baseUrl = properties.getBaseUrl();
        this.internalToken = jwtAuthProperties.getInternalAccessToken();
    }

    public CatalogProductView getProduct(UUID productId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            if (internalToken != null && !internalToken.isBlank()) {
                headers.set("X-Internal-Token", internalToken);
            }
            ResponseEntity<ApiResponse<CatalogProductView>> response = restTemplate.exchange(
                    baseUrl + "/catalog/products/{productId}",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    RESPONSE_TYPE,
                    productId
            );
            ApiResponse<CatalogProductView> body = response.getBody();
            if (body == null || body.data() == null) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "商品服务返回为空");
            }
            if (body.code() != ErrorCode.SUCCESS.code()) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "商品服务返回异常: " + body.message());
            }
            return body.data();
        } catch (HttpClientErrorException.NotFound ex) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "商品不存在或已下架");
        } catch (RestClientException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "商品服务调用失败");
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
        public record CatalogProductView(
            UUID id,
            UUID vendorId,
            String name,
            List<RentalPlanView> rentalPlans
        ) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record RentalPlanView(
                UUID id,
            String planType,
            Integer termMonths,
            java.math.BigDecimal depositAmount,
            java.math.BigDecimal rentAmountMonthly,
            java.math.BigDecimal buyoutPrice,
            List<SkuView> skus
        ) {
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record SkuView(UUID id, String skuCode) {
        }
    }
}
