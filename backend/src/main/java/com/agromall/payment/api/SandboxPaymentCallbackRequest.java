package com.agromall.payment.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record SandboxPaymentCallbackRequest(@NotBlank String paymentNo, @NotBlank String orderNo,
                                             @NotNull @DecimalMin("0.00") BigDecimal amount,
                                             @NotBlank String result, @NotNull Long timestamp,
                                             @NotBlank String signature) {}
