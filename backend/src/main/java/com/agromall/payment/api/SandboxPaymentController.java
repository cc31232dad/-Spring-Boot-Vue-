package com.agromall.payment.api;

import com.agromall.common.api.ApiResponse;
import com.agromall.auth.security.JwtService;
import com.agromall.payment.application.PaymentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
public class SandboxPaymentController {
    private final PaymentService paymentService;

    public SandboxPaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/api/payments/sandbox/callback")
    public ApiResponse<PaymentView> callback(@Valid @RequestBody SandboxPaymentCallbackRequest request) {
        return ApiResponse.ok(paymentService.callback(request));
    }

    @PostMapping("/api/payments/sandbox/simulate")
    public ApiResponse<PaymentView> simulate(@AuthenticationPrincipal JwtService.JwtPrincipal principal,
                                             @Valid @RequestBody SandboxPaymentSimulationRequest request) {
        return ApiResponse.ok(paymentService.simulate(principal.userId(), request));
    }
}
