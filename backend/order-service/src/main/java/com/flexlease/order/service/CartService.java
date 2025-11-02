package com.flexlease.order.service;

import com.flexlease.common.exception.BusinessException;
import com.flexlease.common.exception.ErrorCode;
import com.flexlease.common.security.FlexleasePrincipal;
import com.flexlease.common.security.SecurityUtils;
import com.flexlease.order.domain.CartItem;
import com.flexlease.order.dto.AddCartItemRequest;
import com.flexlease.order.dto.CartItemResponse;
import com.flexlease.order.dto.UpdateCartItemRequest;
import com.flexlease.order.repository.CartItemRepository;
import jakarta.transaction.Transactional;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class CartService {

    private final CartItemRepository cartItemRepository;

    public CartService(CartItemRepository cartItemRepository) {
        this.cartItemRepository = cartItemRepository;
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public List<CartItemResponse> listCartItems(UUID userId) {
        requireUserAccess(userId);
        return cartItemRepository.findByUserIdOrderByCreatedAtAsc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    public CartItemResponse addItem(AddCartItemRequest request) {
        requireUserAccess(request.userId());
        CartItem item = cartItemRepository.findByUserIdAndSkuId(request.userId(), request.skuId())
                .map(existing -> {
                    existing.increaseQuantity(request.quantity());
                    existing.refreshDetails(request.vendorId(), request.productId(), request.planId(),
                            request.productName(), request.skuCode(), request.planSnapshot(),
                            request.unitRentAmount(), request.unitDepositAmount(), request.buyoutPrice());
                    return existing;
                })
                .orElseGet(() -> CartItem.create(
                        request.userId(),
                        request.vendorId(),
                        request.productId(),
                        request.skuId(),
                        request.planId(),
                        request.productName(),
                        request.skuCode(),
                        request.planSnapshot(),
                        request.quantity(),
                        request.unitRentAmount(),
                        request.unitDepositAmount(),
                        request.buyoutPrice()
                ));
        CartItem saved = cartItemRepository.save(item);
        return toResponse(saved);
    }

    public CartItemResponse updateItem(UUID itemId, UpdateCartItemRequest request) {
        requireUserAccess(request.userId());
        CartItem item = cartItemRepository.findByIdAndUserId(itemId, request.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "购物车条目不存在"));
        item.updateQuantity(request.quantity());
        return toResponse(item);
    }

    public void removeItem(UUID userId, UUID itemId) {
        requireUserAccess(userId);
        CartItem item = cartItemRepository.findByIdAndUserId(itemId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "购物车条目不存在"));
        cartItemRepository.delete(item);
    }

    public void clearCart(UUID userId) {
        requireUserAccess(userId);
        cartItemRepository.deleteByUserId(userId);
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public List<CartItem> loadCartItems(UUID userId, List<UUID> itemIds) {
        requireUserAccess(userId);
        if (itemIds == null || itemIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<CartItem> rawItems = cartItemRepository.findAllById(itemIds);
        if (rawItems.size() != itemIds.size()) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "部分购物车条目不存在");
        }
        Map<UUID, CartItem> mapped = rawItems.stream()
                .collect(java.util.stream.Collectors.toMap(CartItem::getId, Function.identity()));
        List<CartItem> items = itemIds.stream()
                .map(id -> {
                    CartItem item = mapped.get(id);
                    if (item == null) {
                        throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "购物车条目不存在");
                    }
                    return item;
                })
                .toList();
        boolean belongsToUser = items.stream().allMatch(item -> item.getUserId().equals(userId));
        if (!belongsToUser) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "存在不属于当前用户的购物车条目");
        }
        return items;
    }

    public void removeItems(UUID userId, List<UUID> itemIds) {
        requireUserAccess(userId);
        if (itemIds == null || itemIds.isEmpty()) {
            return;
        }
        cartItemRepository.deleteByUserIdAndIdIn(userId, itemIds);
    }

    private void requireUserAccess(UUID userId) {
        FlexleasePrincipal principal = SecurityUtils.getCurrentPrincipal()
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "未认证"));
        if (principal.hasRole("ADMIN") || principal.hasRole("INTERNAL")) {
            return;
        }
        if (principal.userId() == null || !principal.userId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该购物车");
        }
    }

    private CartItemResponse toResponse(CartItem item) {
        return new CartItemResponse(
                item.getId(),
                item.getUserId(),
                item.getVendorId(),
                item.getProductId(),
                item.getSkuId(),
                item.getPlanId(),
                item.getProductName(),
                item.getSkuCode(),
                item.getPlanSnapshot(),
                item.getQuantity(),
                item.getUnitRentAmount(),
                item.getUnitDepositAmount(),
                item.getBuyoutPrice(),
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }
}
