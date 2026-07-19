package com.agromall.order.api;

import com.agromall.auth.security.JwtService;
import com.agromall.common.api.ApiResponse;
import com.agromall.order.application.OrderService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/api/orders/checkout")
    public ApiResponse<List<OrderView>> checkout(@AuthenticationPrincipal JwtService.JwtPrincipal principal,
                                                 @Valid @RequestBody CheckoutRequest request) {
        return ApiResponse.ok(orderService.checkout(principal.userId(), request));
    }

    @GetMapping("/api/orders/my")
    public ApiResponse<List<OrderView>> myOrders(@AuthenticationPrincipal JwtService.JwtPrincipal principal) {
        return ApiResponse.ok(orderService.getMyOrders(principal.userId()));
    }

    @GetMapping("/api/orders/{id}")
    public ApiResponse<OrderView> detail(@AuthenticationPrincipal JwtService.JwtPrincipal principal,
                                         @PathVariable Long id) {
        return ApiResponse.ok(orderService.getMyOrder(principal.userId(), id));
    }

    @PatchMapping("/api/orders/{id}/cancel")
    public ApiResponse<OrderView> cancel(@AuthenticationPrincipal JwtService.JwtPrincipal principal,
                                         @PathVariable Long id) {
        return ApiResponse.ok(orderService.cancel(principal.userId(), id));
    }

    @PatchMapping("/api/orders/{id}/complete")
    public ApiResponse<OrderView> complete(@AuthenticationPrincipal JwtService.JwtPrincipal principal,
                                           @PathVariable Long id) {
        return ApiResponse.ok(orderService.complete(principal.userId(), id));
    }
}
