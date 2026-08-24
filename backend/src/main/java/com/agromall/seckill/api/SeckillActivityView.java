package com.agromall.seckill.api;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SeckillActivityView(Long id, Long productId, Long farmerId, String status,
                                  BigDecimal seckillPrice, Integer totalStock, Integer limitPerUser,
                                  LocalDateTime startAt, LocalDateTime endAt) {
}
