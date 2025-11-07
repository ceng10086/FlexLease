package com.flexlease.order.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flexlease.common.dto.ApiResponse;
import com.flexlease.common.exception.BusinessException;
import com.flexlease.common.exception.ErrorCode;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class InventoryReservationClient {

    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";
    private static final ParameterizedTypeReference<ApiResponse<Void>> RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String internalToken;
    private final ObjectMapper objectMapper;

    public InventoryReservationClient(RestTemplate restTemplate,
                                      ProductServiceProperties properties,
                                      @Value("${security.jwt.internal-access-token}") String internalToken,
                                      ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.baseUrl = properties.getBaseUrl();
        this.internalToken = internalToken;
        this.objectMapper = objectMapper;
    }

    public void reserve(UUID referenceId, List<InventoryCommand> commands) {
        dispatch(referenceId, commands, InventoryOperation.RESERVE);
    }

    public void release(UUID referenceId, List<InventoryCommand> commands) {
        dispatch(referenceId, commands, InventoryOperation.RELEASE);
    }

    public void outbound(UUID referenceId, List<InventoryCommand> commands) {
        dispatch(referenceId, commands, InventoryOperation.OUTBOUND);
    }

    public void inbound(UUID referenceId, List<InventoryCommand> commands) {
        dispatch(referenceId, commands, InventoryOperation.INBOUND);
    }

    private void dispatch(UUID referenceId, List<InventoryCommand> commands, InventoryOperation operation) {
        if (commands == null || commands.isEmpty()) {
            return;
        }
        Payload payload = new Payload(
                referenceId,
                commands.stream()
                        .map(cmd -> new PayloadItem(cmd.skuId(), cmd.quantity(), operation))
                        .toList()
        );
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set(INTERNAL_TOKEN_HEADER, internalToken);
            
            ResponseEntity<ApiResponse<Void>> response = restTemplate.exchange(
                    baseUrl + "/internal/inventory/reservations",
                    HttpMethod.POST,
                    new HttpEntity<>(payload, headers),
                    RESPONSE_TYPE
            );
            ApiResponse<Void> body = response.getBody();
            if (body != null && body.code() != ErrorCode.SUCCESS.code()) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, body.message());
            }
        } catch (HttpStatusCodeException ex) {
            ApiResponse<?> errorBody = parseErrorResponse(ex.getResponseBodyAsString());
            ErrorCode fallback = ex.getStatusCode().is4xxClientError()
                    ? ErrorCode.VALIDATION_ERROR
                    : ErrorCode.INTERNAL_ERROR;
            ErrorCode mappedCode = resolveErrorCode(errorBody, fallback);
            String message = resolveErrorMessage(ex, errorBody);
            throw new BusinessException(mappedCode, message);
        } catch (RestClientException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "库存服务调用失败");
        }
    }

    private ApiResponse<?> parseErrorResponse(String rawBody) {
        if (rawBody == null || rawBody.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(rawBody, new TypeReference<ApiResponse<Object>>() {
            });
        } catch (Exception ignored) {
            return null;
        }
    }

    private ErrorCode resolveErrorCode(ApiResponse<?> errorBody, ErrorCode fallback) {
        if (errorBody == null) {
            return fallback;
        }
        int code = errorBody.code();
        for (ErrorCode candidate : ErrorCode.values()) {
            if (candidate.code() == code) {
                return candidate;
            }
        }
        return fallback;
    }

    private String resolveErrorMessage(HttpStatusCodeException ex, ApiResponse<?> errorBody) {
        if (errorBody != null && errorBody.message() != null && !errorBody.message().isBlank()) {
            return errorBody.message();
        }
        String responseBody = ex.getResponseBodyAsString();
        if (responseBody != null && !responseBody.isBlank()) {
            return responseBody;
        }
        return ex.getStatusText();
    }

    public record InventoryCommand(UUID skuId, int quantity) {
    }

    private record Payload(UUID referenceId, List<PayloadItem> items) {
    }

    private record PayloadItem(UUID skuId, int quantity, InventoryOperation changeType) {
    }

    public enum InventoryOperation {
        RESERVE,
        RELEASE,
        OUTBOUND,
        INBOUND
    }
}
