package com.agromall.seckill.api;

import jakarta.validation.constraints.NotBlank;

public record SeckillRequest(@NotBlank String receiverName, @NotBlank String receiverPhone,
                             @NotBlank String receiverAddress) {
}
