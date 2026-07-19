package com.agromall.cart.api;

import java.math.BigDecimal;

public record CartItemView(Long id, Long productId, String productName, BigDecimal price,
                           Integer quantity, Integer stock, String originPlace, String imageUrl,
                           Long farmerId, BigDecimal subtotal) {
}
