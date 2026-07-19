package com.agromall.product.api;

import java.math.BigDecimal;

public record ProductDetailView(Long id, Long categoryId, String categoryName, Long farmerId,
                                String name, String description, BigDecimal price, Integer stock,
                                String originPlace, String imageUrl, String status) {
}
