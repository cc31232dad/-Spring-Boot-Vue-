package com.agromall.order.api;

import java.math.BigDecimal;

public record OrderItemView(Long id, Long productId, String productName, String productImageUrl,
                            String originPlace, BigDecimal unitPrice, Integer quantity,
                            BigDecimal subtotal) {}
