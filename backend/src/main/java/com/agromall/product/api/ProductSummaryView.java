package com.agromall.product.api;

import java.math.BigDecimal;

public record ProductSummaryView(Long id, Long categoryId, String categoryName, String name,
                                 BigDecimal price, Integer stock, String originPlace, String imageUrl) {
}
