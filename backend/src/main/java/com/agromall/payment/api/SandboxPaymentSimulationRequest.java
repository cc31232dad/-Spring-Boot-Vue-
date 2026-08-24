package com.agromall.payment.api;

import jakarta.validation.constraints.NotBlank;

public record SandboxPaymentSimulationRequest(@NotBlank String paymentNo, @NotBlank String result) {}
