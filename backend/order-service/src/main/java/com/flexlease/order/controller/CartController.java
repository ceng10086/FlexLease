package com.flexlease.order.controller;

import com.flexlease.common.dto.ApiResponse;
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

@RestController
@RequestMapping("/api/v1/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public ApiResponse<List<CartItemResponse>> list(@RequestParam UUID userId) {
        return ApiResponse.success(cartService.listCartItems(userId));
    }

    @PostMapping("/items")
    public ApiResponse<CartItemResponse> addItem(@Valid @RequestBody AddCartItemRequest request) {
        return ApiResponse.success(cartService.addItem(request));
    }

    @PutMapping("/items/{itemId}")
    public ApiResponse<CartItemResponse> updateItem(@PathVariable UUID itemId,
                                                    @Valid @RequestBody UpdateCartItemRequest request) {
        return ApiResponse.success(cartService.updateItem(itemId, request));
    }

    @DeleteMapping("/items/{itemId}")
    public ApiResponse<Void> deleteItem(@PathVariable UUID itemId, @RequestParam UUID userId) {
        cartService.removeItem(userId, itemId);
        return ApiResponse.success(null);
    }

    @DeleteMapping
    public ApiResponse<Void> clear(@RequestParam UUID userId) {
        cartService.clearCart(userId);
        return ApiResponse.success(null);
    }
}
