package com.flexlease.order.service;

import com.flexlease.order.domain.OrderExtensionRequest;
import com.flexlease.order.domain.OrderEvent;
import com.flexlease.order.domain.OrderReturnRequest;
import com.flexlease.order.domain.RentalOrder;
import com.flexlease.order.domain.RentalOrderItem;
import com.flexlease.order.dto.OrderEventResponse;
import com.flexlease.order.dto.OrderExtensionResponse;
import com.flexlease.order.dto.OrderReturnResponse;
import com.flexlease.order.dto.RentalOrderItemResponse;
import com.flexlease.order.dto.RentalOrderResponse;
import com.flexlease.order.dto.RentalOrderSummaryResponse;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class OrderAssembler {

    public RentalOrderResponse toOrderResponse(RentalOrder order) {
        return new RentalOrderResponse(
                order.getId(),
                order.getOrderNo(),
                order.getUserId(),
                order.getVendorId(),
                order.getStatus(),
                order.getPlanType(),
                order.getTotalAmount(),
                order.getDepositAmount(),
                order.getRentAmount(),
                order.getBuyoutAmount(),
                order.getLeaseStartAt(),
                order.getLeaseEndAt(),
                order.getExtensionCount(),
                order.getShippingCarrier(),
                order.getShippingTrackingNo(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                order.getItems().stream().map(this::toItemResponse).toList(),
                order.getEvents().stream()
                        .sorted(Comparator.comparing(OrderEvent::getCreatedAt))
                        .map(this::toEventResponse)
                        .toList(),
                order.getExtensionRequests().stream()
                        .sorted(Comparator.comparing(OrderExtensionRequest::getRequestedAt))
                        .map(this::toExtensionResponse)
                        .toList(),
                order.getReturnRequests().stream()
                        .sorted(Comparator.comparing(OrderReturnRequest::getRequestedAt))
                        .map(this::toReturnResponse)
                        .toList()
        );
    }

    public RentalOrderSummaryResponse toSummary(RentalOrder order) {
        return new RentalOrderSummaryResponse(
                order.getId(),
                order.getOrderNo(),
                order.getUserId(),
                order.getVendorId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getDepositAmount(),
                order.getRentAmount(),
                order.getCreatedAt(),
                order.getLeaseEndAt()
        );
    }

    private RentalOrderItemResponse toItemResponse(RentalOrderItem item) {
        return new RentalOrderItemResponse(
                item.getId(),
                item.getProductId(),
                item.getSkuId(),
                item.getPlanId(),
                item.getProductName(),
                item.getSkuCode(),
                item.getPlanSnapshot(),
                item.getQuantity(),
                item.getUnitRentAmount(),
                item.getUnitDepositAmount(),
                item.getBuyoutPrice()
        );
    }

    private OrderEventResponse toEventResponse(OrderEvent event) {
        return new OrderEventResponse(
                event.getId(),
                event.getEventType(),
                event.getDescription(),
                event.getCreatedBy(),
                event.getCreatedAt()
        );
    }

    private OrderExtensionResponse toExtensionResponse(OrderExtensionRequest request) {
        return new OrderExtensionResponse(
                request.getId(),
                request.getStatus(),
                request.getAdditionalMonths(),
                request.getRequestedBy(),
                request.getRequestedAt(),
                request.getDecisionBy(),
                request.getDecisionAt(),
                request.getRemark()
        );
    }

    private OrderReturnResponse toReturnResponse(OrderReturnRequest request) {
        return new OrderReturnResponse(
                request.getId(),
                request.getStatus(),
                request.getReason(),
                request.getLogisticsCompany(),
                request.getTrackingNumber(),
                request.getRequestedBy(),
                request.getRequestedAt(),
                request.getDecisionBy(),
                request.getDecisionAt(),
                request.getRemark()
        );
    }
}
