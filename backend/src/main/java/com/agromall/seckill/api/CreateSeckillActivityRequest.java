package com.agromall.seckill.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreateSeckillActivityRequest(@NotNull Long productId, @NotNull @DecimalMin("0.00") BigDecimal seckillPrice,
                                           @NotNull @Min(1) Integer totalStock, @NotNull @Future LocalDateTime startAt,
                                           @NotNull @Future LocalDateTime endAt) {
}
