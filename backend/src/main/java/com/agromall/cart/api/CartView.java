package com.agromall.cart.api;

import java.math.BigDecimal;
import java.util.List;

public record CartView(List<CartItemView> items, BigDecimal totalAmount) {
}
