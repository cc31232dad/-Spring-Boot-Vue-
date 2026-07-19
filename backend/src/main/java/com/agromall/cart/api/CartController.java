package com.agromall.cart.api;

import com.agromall.auth.security.JwtService;
import com.agromall.cart.application.CartService;
import com.agromall.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping("/api/cart")
    public ApiResponse<CartView> getCart(@AuthenticationPrincipal JwtService.JwtPrincipal principal) {
        return ApiResponse.ok(cartService.getCart(principal.userId()));
    }

    @PostMapping("/api/cart/items")
    public ApiResponse<CartView> addItem(@AuthenticationPrincipal JwtService.JwtPrincipal principal,
                                         @Valid @RequestBody AddCartItemRequest request) {
        return ApiResponse.ok(cartService.addItem(principal.userId(), request));
    }

    @PutMapping("/api/cart/items/{id}")
    public ApiResponse<CartView> updateItem(@AuthenticationPrincipal JwtService.JwtPrincipal principal,
                                            @PathVariable Long id,
                                            @Valid @RequestBody UpdateCartItemRequest request) {
        return ApiResponse.ok(cartService.updateItem(principal.userId(), id, request));
    }

    @DeleteMapping("/api/cart/items/{id}")
    public ApiResponse<Void> deleteItem(@AuthenticationPrincipal JwtService.JwtPrincipal principal,
                                        @PathVariable Long id) {
        cartService.deleteItem(principal.userId(), id);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/api/cart")
    public ApiResponse<Void> clearCart(@AuthenticationPrincipal JwtService.JwtPrincipal principal) {
        cartService.clearCart(principal.userId());
        return ApiResponse.ok(null);
    }
}
