package com.flexlease.order.service;

import com.flexlease.common.exception.BusinessException;
import com.flexlease.common.exception.ErrorCode;
import com.flexlease.common.notification.NotificationChannel;
import com.flexlease.common.notification.NotificationSendRequest;
import com.flexlease.common.security.FlexleasePrincipal;
import com.flexlease.common.security.SecurityUtils;
import com.flexlease.order.client.NotificationClient;
import com.flexlease.order.domain.OrderActorRole;
import com.flexlease.order.domain.OrderEventType;
import com.flexlease.order.domain.OrderProof;
import com.flexlease.order.domain.OrderProofType;
import com.flexlease.order.domain.RentalOrder;
import com.flexlease.order.dto.OrderProofResponse;
import com.flexlease.order.repository.OrderProofRepository;
import com.flexlease.order.repository.RentalOrderRepository;
import com.flexlease.order.storage.ProofStorageService;
import com.flexlease.order.storage.ProofStorageService.StoredFile;
import jakarta.transaction.Transactional;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;

/**
 * 订单取证服务：负责上传/读取取证文件，并把取证动作写入订单时间线。
 * <p>
 * 上传成功后会通知对方（站内信）；巡检类取证会触发信用奖励。
 */
@Service
@Transactional
public class OrderProofService {

    private static final Set<OrderProofType> USER_ALLOWED_TYPES = EnumSet.of(
        OrderProofType.RECEIVE,
        OrderProofType.RETURN,
        OrderProofType.INSPECTION,
        OrderProofType.OTHER
    );

    private static final Set<OrderProofType> VENDOR_ALLOWED_TYPES = EnumSet.of(
        OrderProofType.SHIPMENT,
        OrderProofType.RETURN,
        OrderProofType.INSPECTION,
        OrderProofType.OTHER
    );

    private final RentalOrderRepository rentalOrderRepository;
    private final ProofStorageService proofStorageService;
    private final OrderAssembler orderAssembler;
    private final OrderTimelineService timelineService;
    private final NotificationClient notificationClient;
    private final OrderProofRepository orderProofRepository;
    private final CreditRewardService creditRewardService;

    public OrderProofService(RentalOrderRepository rentalOrderRepository,
                             ProofStorageService proofStorageService,
                             OrderAssembler orderAssembler,
                             OrderTimelineService timelineService,
                             NotificationClient notificationClient,
                             OrderProofRepository orderProofRepository,
                             CreditRewardService creditRewardService) {
        this.rentalOrderRepository = rentalOrderRepository;
        this.proofStorageService = proofStorageService;
        this.orderAssembler = orderAssembler;
        this.timelineService = timelineService;
        this.notificationClient = notificationClient;
        this.orderProofRepository = orderProofRepository;
        this.creditRewardService = creditRewardService;
    }

    public List<OrderProofResponse> list(UUID orderId) {
        RentalOrder order = loadOrder(orderId);
        ensureReadable(order);
        return order.getProofs().stream()
                .sorted(Comparator.comparing(OrderProof::getUploadedAt))
                .map(orderAssembler::toProofResponse)
                .toList();
    }

    public OrderProofResponse upload(UUID orderId,
                                     UUID actorId,
                                     OrderProofType proofType,
                                     String description,
                                     MultipartFile file) {
        if (actorId == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "缺少操作人");
        }
        if (proofType == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "请选择取证类型");
        }
        RentalOrder order = loadOrder(orderId);
        OrderActorRole actorRole = resolveActorRole(order, actorId);
        ensureProofTypeAllowed(actorRole, proofType);
        StoredFile stored = proofStorageService.store(file);
        try {
            String watermark = "订单 %s %s".formatted(
                    order.getOrderNo(),
                    OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
            );
            proofStorageService.applyWatermark(stored.storedName(), stored.contentType(), watermark);
        } catch (RuntimeException ex) {
            // 水印尽力而为，不阻塞上传
        }
        try {
            OrderProof proof = OrderProof.create(
                    proofType,
                    StringUtils.hasText(description) ? description.trim() : null,
                    stored.storedName(),
                    stored.fileUrl(),
                    stored.contentType(),
                    stored.size(),
                    actorId,
                    actorRole
            );
            order.addProof(proof);
            Map<String, Object> attributes = Map.of(
                    "proofType", proofType.name(),
                    "fileUrl", stored.fileUrl()
            );
            String eventMessage = "[%s] 上传取证材料".formatted(proofType.name());
            timelineService.append(order, OrderEventType.PROOF_UPLOADED, eventMessage, actorId, attributes, actorRole);
            notifyCounterparty(order, actorRole, proofType);
            maybeRewardInspection(order, actorRole, proofType, actorId);
            return orderAssembler.toProofResponse(proof);
        } catch (RuntimeException ex) {
            proofStorageService.delete(stored.storedName());
            throw ex;
        }
    }

    private void maybeRewardInspection(RentalOrder order,
                                       OrderActorRole actorRole,
                                       OrderProofType proofType,
                                       UUID actorId) {
        if (actorRole != OrderActorRole.USER || proofType != OrderProofType.INSPECTION) {
            return;
        }
        boolean requested = order.getEvents().stream()
                .anyMatch(event -> event.getEventType() == OrderEventType.INSPECTION_REQUESTED);
        if (!requested) {
            return;
        }
        boolean rewarded = order.getEvents().stream()
                .anyMatch(event -> event.getEventType() == OrderEventType.INSPECTION_REWARDED);
        if (rewarded) {
            return;
        }
        creditRewardService.rewardInspectionCooperation(order);
        timelineService.append(order,
                OrderEventType.INSPECTION_REWARDED,
                "巡检配合奖励：信用积分 +2",
                actorId,
                Map.of("creditDelta", 2),
                actorRole);
    }

    public ProofFileResource loadProofFile(String storedName) {
        OrderProof proof = orderProofRepository.findByFileName(storedName)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "取证文件不存在"));
        RentalOrder order = proof.getOrder();
        ensureReadable(order);
        Resource resource = proofStorageService.loadAsResource(storedName);
        String contentType = proof.getContentType() != null ? proof.getContentType() : "application/octet-stream";
        long actualSize = proof.getFileSize();
        try {
            actualSize = resource.contentLength();
        } catch (Exception ignored) {
            // 读取真实大小失败时回退到数据库记录
        }
        return new ProofFileResource(storedName, contentType, actualSize, resource);
    }

    private void ensureProofTypeAllowed(OrderActorRole role, OrderProofType type) {
        if (role == OrderActorRole.USER && !USER_ALLOWED_TYPES.contains(type)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "当前角色无权上传该类型的取证资料");
        }
        if (role == OrderActorRole.VENDOR && !VENDOR_ALLOWED_TYPES.contains(type)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "厂商无权上传该类型的取证资料");
        }
    }

    private void notifyCounterparty(RentalOrder order, OrderActorRole actorRole, OrderProofType type) {
        String subject;
        String content;
        if (actorRole == OrderActorRole.USER) {
            subject = "用户上传" + type.name() + "凭证";
            content = "订单 %s 用户已上传新的取证资料，请及时查看。".formatted(order.getOrderNo());
            notifyVendor(order, subject, content);
        } else if (actorRole == OrderActorRole.VENDOR) {
            subject = "厂商上传" + type.name() + "凭证";
            content = "订单 %s 厂商已上传取证资料，请前往订单详情查阅。".formatted(order.getOrderNo());
            notifyUser(order, subject, content);
        }
    }

    private RentalOrder loadOrder(UUID orderId) {
        return rentalOrderRepository.findByIdWithDetails(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "订单不存在"));
    }

    private void ensureReadable(RentalOrder order) {
        FlexleasePrincipal principal = SecurityUtils.requirePrincipal();
        if (principal.hasRole("ADMIN") || principal.hasRole("INTERNAL")) {
            return;
        }
        if (principal.hasRole("VENDOR")) {
            UUID vendorId = principal.vendorId();
            if (vendorId == null || !vendorId.equals(order.getVendorId())) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "无权查看该订单取证资料");
            }
            return;
        }
        if (principal.hasRole("USER")) {
            UUID userId = principal.userId();
            if (userId == null || !userId.equals(order.getUserId())) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "无权查看该订单取证资料");
            }
            return;
        }
        throw new BusinessException(ErrorCode.FORBIDDEN, "缺少访问取证资料的权限");
    }

    private OrderActorRole resolveActorRole(RentalOrder order, UUID actorId) {
        FlexleasePrincipal principal = SecurityUtils.requirePrincipal();
        UUID currentUserId = principal.userId();
        if (currentUserId == null || !currentUserId.equals(actorId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "请求用户与当前登录用户不一致");
        }
        if (principal.hasRole("ADMIN")) {
            return OrderActorRole.ADMIN;
        }
        if (principal.hasRole("INTERNAL")) {
            return OrderActorRole.INTERNAL;
        }
        if (principal.hasRole("VENDOR")) {
            UUID vendorId = principal.vendorId();
            if (vendorId == null || !vendorId.equals(order.getVendorId())) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "无权上传该订单取证资料");
            }
            return OrderActorRole.VENDOR;
        }
        if (principal.hasRole("USER")) {
            if (!order.getUserId().equals(actorId)) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "无权上传该订单取证资料");
            }
            return OrderActorRole.USER;
        }
        throw new BusinessException(ErrorCode.FORBIDDEN, "缺少上传取证资料的权限");
    }

    private void notifyUser(RentalOrder order, String subject, String content) {
        NotificationSendRequest request = new NotificationSendRequest(
                null,
                NotificationChannel.IN_APP,
                order.getUserId().toString(),
                subject,
                content,
                Map.of("orderNo", order.getOrderNo())
        );
        try {
            notificationClient.send(request);
        } catch (RuntimeException ex) {
            // 降级处理：通知失败不影响主流程
        }
    }

    private void notifyVendor(RentalOrder order, String subject, String content) {
        NotificationSendRequest request = new NotificationSendRequest(
                null,
                NotificationChannel.IN_APP,
                order.getVendorId().toString(),
                subject,
                content,
                Map.of("orderNo", order.getOrderNo())
        );
        try {
            notificationClient.send(request);
        } catch (RuntimeException ex) {
            // 忽略通知异常
        }
    }

    public record ProofFileResource(String fileName,
                                    String contentType,
                                    long fileSize,
                                    Resource resource) {
    }
}
