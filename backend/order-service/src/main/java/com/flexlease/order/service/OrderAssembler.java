package com.flexlease.order.service;

import com.flexlease.order.config.ProofPolicyProperties;
import com.flexlease.order.domain.OrderDispute;
import com.flexlease.order.domain.OrderExtensionRequest;
import com.flexlease.order.domain.OrderEvent;
import com.flexlease.order.domain.OrderProof;
import com.flexlease.order.domain.OrderReturnRequest;
import com.flexlease.order.domain.OrderSatisfactionSurvey;
import com.flexlease.order.domain.RentalOrder;
import com.flexlease.order.domain.RentalOrderItem;
import com.flexlease.order.dto.OrderDisputeResponse;
import com.flexlease.order.dto.OrderEventResponse;
import com.flexlease.order.dto.OrderExtensionResponse;
import com.flexlease.order.dto.OrderProofResponse;
import com.flexlease.order.dto.OrderReturnResponse;
import com.flexlease.order.dto.OrderSurveyResponse;
import com.flexlease.order.dto.RentalOrderItemResponse;
import com.flexlease.order.dto.RentalOrderResponse;
import com.flexlease.order.dto.RentalOrderSummaryResponse;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class OrderAssembler {

        private final int shipmentPhotoRequired;
        private final int shipmentVideoRequired;

        public OrderAssembler(ProofPolicyProperties proofPolicyProperties) {
                this.shipmentPhotoRequired = Math.max(0, proofPolicyProperties.getShipmentPhotoRequired());
                this.shipmentVideoRequired = Math.max(0, proofPolicyProperties.getShipmentVideoRequired());
        }

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
                order.getOriginalDepositAmount(),
                order.getRentAmount(),
                order.getBuyoutAmount(),
                order.getCreditScore(),
                order.getCreditTier(),
                order.getDepositAdjustmentRate(),
                order.isRequiresManualReview(),
                order.getPaymentTransactionId(),
                order.getLeaseStartAt(),
                order.getLeaseEndAt(),
                order.getExtensionCount(),
                order.getShippingCarrier(),
                order.getShippingTrackingNo(),
                order.getCustomerRemark(),
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
                        .toList(),
                order.getProofs().stream()
                        .sorted(Comparator.comparing(OrderProof::getUploadedAt))
                        .map(this::toProofResponse)
                        .toList(),
                order.getDisputes().stream()
                        .sorted(Comparator.comparing(OrderDispute::getCreatedAt))
                        .map(this::toDisputeResponse)
                        .toList(),
                order.getSurveys().stream()
                        .sorted(Comparator.comparing(OrderSatisfactionSurvey::getRequestedAt))
                        .map(this::toSurveyResponse)
                        .toList(),
                shipmentPhotoRequired,
                shipmentVideoRequired
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
                event.getActorRole(),
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

    public OrderProofResponse toProofResponse(OrderProof proof) {
        return new OrderProofResponse(
                proof.getId(),
                proof.getProofType(),
                proof.getDescription(),
                proof.getFileUrl(),
                proof.getContentType(),
                proof.getFileSize(),
                proof.getUploadedBy(),
                proof.getActorRole(),
                proof.getUploadedAt()
        );
    }

    public OrderDisputeResponse toDisputeResponse(OrderDispute dispute) {
        return new OrderDisputeResponse(
                dispute.getId(),
                dispute.getStatus(),
                dispute.getInitiatorId(),
                dispute.getInitiatorRole(),
                dispute.getInitiatorOption(),
                dispute.getInitiatorReason(),
                dispute.getInitiatorRemark(),
                dispute.getRespondentId(),
                dispute.getRespondentRole(),
                dispute.getRespondentOption(),
                dispute.getRespondentRemark(),
                dispute.getRespondedAt(),
                dispute.getDeadlineAt(),
                dispute.getEscalatedBy(),
                dispute.getEscalatedAt(),
                dispute.getAdminDecisionOption(),
                dispute.getAdminDecisionRemark(),
                dispute.getAdminDecisionBy(),
                dispute.getAdminDecisionAt(),
                dispute.getUserCreditDelta(),
                dispute.getAppealCount(),
                dispute.getCreatedAt(),
                dispute.getUpdatedAt()
        );
    }

    public OrderSurveyResponse toSurveyResponse(OrderSatisfactionSurvey survey) {
        return new OrderSurveyResponse(
                survey.getId(),
                survey.getDispute() == null ? null : survey.getDispute().getId(),
                survey.getStatus(),
                survey.getTargetRole(),
                survey.getTargetRef(),
                survey.getRating(),
                survey.getComment(),
                survey.getRequestedAt(),
                survey.getAvailableAt(),
                survey.getSubmittedAt()
        );
    }
}
