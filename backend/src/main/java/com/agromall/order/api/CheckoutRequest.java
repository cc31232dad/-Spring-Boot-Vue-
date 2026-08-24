package com.agromall.order.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CheckoutRequest(
        @NotEmpty List<Long> cartItemIds,
        @NotBlank @Size(max = 50) String receiverName,
        @NotBlank @Size(max = 30) String receiverPhone,
        @NotBlank @Size(max = 255) String receiverAddress
) {}
