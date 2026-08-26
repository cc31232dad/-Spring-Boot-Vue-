package com.agromall.payment.api;

import com.agromall.auth.security.JwtService;
import com.agromall.common.api.ApiResponse;
import com.agromall.payment.application.PaymentService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/api/orders/{orderId}/payment")
    public ApiResponse<PaymentView> create(@AuthenticationPrincipal JwtService.JwtPrincipal principal,
                                           @PathVariable Long orderId) {
        return ApiResponse.ok(paymentService.create(principal.userId(), orderId));
    }
}
