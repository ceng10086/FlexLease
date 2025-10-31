package com.flexlease.order.client;

import com.flexlease.common.dto.ApiResponse;
import com.flexlease.common.exception.BusinessException;
import com.flexlease.common.exception.ErrorCode;
import java.util.List;
import java.util.UUID;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class InventoryReservationClient {

    private static final ParameterizedTypeReference<ApiResponse<Void>> RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public InventoryReservationClient(RestTemplate restTemplate, ProductServiceProperties properties) {
        this.restTemplate = restTemplate;
        this.baseUrl = properties.getBaseUrl();
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
            ResponseEntity<ApiResponse<Void>> response = restTemplate.exchange(
                    baseUrl + "/internal/inventory/reservations",
                    HttpMethod.POST,
                    new HttpEntity<>(payload),
                    RESPONSE_TYPE
            );
            ApiResponse<Void> body = response.getBody();
            if (body != null && body.code() != ErrorCode.SUCCESS.code()) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, body.message());
            }
        } catch (HttpStatusCodeException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "库存服务调用失败：" + ex.getResponseBodyAsString());
        } catch (RestClientException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "库存服务调用失败");
        }
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
        OUTBOUND
    }
}
