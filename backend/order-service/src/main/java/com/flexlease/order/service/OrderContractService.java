package com.flexlease.order.service;

import com.flexlease.common.exception.BusinessException;
import com.flexlease.common.exception.ErrorCode;
import com.flexlease.common.security.FlexleasePrincipal;
import com.flexlease.common.security.SecurityUtils;
import com.flexlease.order.domain.OrderContract;
import com.flexlease.order.domain.OrderContractStatus;
import com.flexlease.order.domain.RentalOrder;
import com.flexlease.order.dto.OrderContractResponse;
import com.flexlease.order.dto.OrderContractSignRequest;
import com.flexlease.order.repository.OrderContractRepository;
import com.flexlease.order.repository.RentalOrderRepository;
import jakarta.transaction.Transactional;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class OrderContractService {

    private final RentalOrderRepository orderRepository;
    private final OrderContractRepository contractRepository;

    public OrderContractService(RentalOrderRepository orderRepository,
                                OrderContractRepository contractRepository) {
        this.orderRepository = orderRepository;
        this.contractRepository = contractRepository;
    }

    public OrderContractResponse getContract(UUID orderId) {
        RentalOrder order = loadOrder(orderId);
        assertReadable(order);
        OrderContract contract = contractRepository.findByOrder_Id(orderId)
                .orElseGet(() -> contractRepository.save(createDraft(order)));
        contract.refreshContent(buildContractContent(order));
        return toResponse(contract);
    }

    public OrderContractResponse signContract(UUID orderId, OrderContractSignRequest request) {
        RentalOrder order = loadOrder(orderId);
        assertSignable(order, request.userId());
        OrderContract contract = contractRepository.findByOrder_Id(orderId)
                .orElseGet(() -> contractRepository.save(createDraft(order)));
        contract.refreshContent(buildContractContent(order));
        contract.sign(request.userId(), request.signature());
        return toResponse(contract);
    }

    private RentalOrder loadOrder(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "订单不存在"));
    }

    private void assertReadable(RentalOrder order) {
        Optional<FlexleasePrincipal> principalOpt = SecurityUtils.getCurrentPrincipal();
        if (principalOpt.isEmpty()) {
            return;
        }
        FlexleasePrincipal principal = principalOpt.get();
        if (principal.hasRole("ADMIN") || principal.hasRole("INTERNAL")) {
            return;
        }
        Optional<UUID> currentUser = SecurityUtils.getCurrentUserId();
        if (currentUser.isPresent() && order.getUserId().equals(currentUser.get())) {
            return;
        }
        if (principal.hasRole("VENDOR")) {
            UUID currentVendorId = principal.vendorId();
            if (currentVendorId == null) {
                throw new BusinessException(ErrorCode.UNAUTHORIZED, "当前身份缺少厂商标识");
            }
            if (!currentVendorId.equals(order.getVendorId())) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该订单合同");
            }
            return;
        }
        throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该订单合同");
    }

    private void assertSignable(RentalOrder order, UUID requestUserId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "未认证用户"));
        if (!order.getUserId().equals(currentUserId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅订单所属用户可以签署合同");
        }
        if (!currentUserId.equals(requestUserId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "签署人信息与当前登录用户不一致");
        }
        FlexleasePrincipal principal = SecurityUtils.getCurrentPrincipal().orElseThrow();
        if (!principal.hasRole("USER") && !principal.hasRole("ADMIN")) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "当前身份不允许签署合同");
        }
    }

    private OrderContract createDraft(RentalOrder order) {
        String contractNumber = generateContractNumber(order.getOrderNo());
        String content = buildContractContent(order);
        return OrderContract.draft(order, contractNumber, content);
    }

    private String generateContractNumber(String orderNo) {
        String prefix = OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd", Locale.CHINA));
        return "LC-" + prefix + "-" + orderNo.substring(Math.max(orderNo.length() - 6, 0));
    }

    private String buildContractContent(RentalOrder order) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.CHINA);
        StringBuilder builder = new StringBuilder();
        builder.append("订单租赁合同\n")
                .append("合同生成时间：").append(formatter.format(OffsetDateTime.now())).append("\n")
                .append("合同关联订单号：").append(order.getOrderNo()).append("\n")
                .append("承租方（用户 ID）：").append(order.getUserId()).append("\n")
                .append("出租方（厂商 ID）：").append(order.getVendorId()).append("\n")
                .append("押金金额：￥").append(order.getDepositAmount()).append("\n")
                .append("租金金额：￥").append(order.getRentAmount()).append("\n");
        if (order.getLeaseStartAt() != null) {
            builder.append("租期开始：").append(formatter.format(order.getLeaseStartAt())).append("\n");
        }
        if (order.getLeaseEndAt() != null) {
            builder.append("租期结束：").append(formatter.format(order.getLeaseEndAt())).append("\n");
        }
        builder.append("合同条款：双方同意按照平台租赁协议履行相应权利义务，逾期归还或违约需承担相应责任。\n");
        return builder.toString();
    }

    private OrderContractResponse toResponse(OrderContract contract) {
        return new OrderContractResponse(
                contract.getId(),
                contract.getOrder().getId(),
                contract.getContractNumber(),
                contract.getStatus(),
                contract.getContent(),
                contract.getSignature(),
                contract.getSignedBy(),
                contract.getGeneratedAt(),
                contract.getSignedAt(),
                contract.getUpdatedAt()
        );
    }
}
