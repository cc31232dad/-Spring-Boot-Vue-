package com.agromall.product.api;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductReviewView(Long id, Long categoryId, String categoryName, Long farmerId,
                                String name, String description, BigDecimal price, Integer stock,
                                String originPlace, String imageUrl, String status,
                                Long reviewedBy, LocalDateTime reviewedAt, String reviewReason) {
}
