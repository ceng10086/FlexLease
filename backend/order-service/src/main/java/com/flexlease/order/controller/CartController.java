package com.flexlease.order.controller;

import com.flexlease.common.dto.ApiResponse;
import com.flexlease.common.exception.BusinessException;
import com.flexlease.common.exception.ErrorCode;
import com.flexlease.common.security.FlexleasePrincipal;
import com.flexlease.common.security.SecurityUtils;
import com.flexlease.order.dto.AddCartItemRequest;
import com.flexlease.order.dto.CartItemResponse;
import com.flexlease.order.dto.UpdateCartItemRequest;
import com.flexlease.order.service.CartService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 购物车接口。
 * <p>
 * 普通用户只能操作自己的购物车；管理员/内部账号可指定 userId 进行排查或演示。
 */
@RestController
@RequestMapping("/api/v1/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public ApiResponse<List<CartItemResponse>> list(@RequestParam(required = false) UUID userId) {
        UUID effectiveUserId = resolveUserId(userId);
        return ApiResponse.success(cartService.listCartItems(effectiveUserId));
    }

    @PostMapping("/items")
    public ApiResponse<CartItemResponse> addItem(@Valid @RequestBody AddCartItemRequest request) {
        UUID effectiveUserId = resolveUserId(request.userId());
        AddCartItemRequest sanitized = new AddCartItemRequest(
                effectiveUserId,
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
        );
        return ApiResponse.success(cartService.addItem(sanitized));
    }

    @PutMapping("/items/{itemId}")
    public ApiResponse<CartItemResponse> updateItem(@PathVariable UUID itemId,
                                                    @Valid @RequestBody UpdateCartItemRequest request) {
        UUID effectiveUserId = resolveUserId(request.userId());
        UpdateCartItemRequest sanitized = new UpdateCartItemRequest(effectiveUserId, request.quantity());
        return ApiResponse.success(cartService.updateItem(itemId, sanitized));
    }

    @DeleteMapping("/items/{itemId}")
    public ApiResponse<Void> deleteItem(@PathVariable UUID itemId, @RequestParam(required = false) UUID userId) {
        UUID effectiveUserId = resolveUserId(userId);
        cartService.removeItem(effectiveUserId, itemId);
        return ApiResponse.success(null);
    }

    @DeleteMapping
    public ApiResponse<Void> clear(@RequestParam(required = false) UUID userId) {
        UUID effectiveUserId = resolveUserId(userId);
        cartService.clearCart(effectiveUserId);
        return ApiResponse.success(null);
    }

    private UUID resolveUserId(UUID requestedUserId) {
        FlexleasePrincipal principal = SecurityUtils.requirePrincipal();
        if (principal.hasRole("ADMIN") || principal.hasRole("INTERNAL")) {
            if (requestedUserId == null) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "管理员操作购物车时需指定 userId");
            }
            return requestedUserId;
        }
        UUID currentUserId = SecurityUtils.requireUserId();
        if (requestedUserId != null && !requestedUserId.equals(currentUserId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "禁止访问其他用户的购物车");
        }
        return currentUserId;
    }
}
