package com.agromall.usercenter.api;

import java.math.BigDecimal;

public record FavoriteView(Long id, Long productId, String name, BigDecimal price, Integer stock,
                           String originPlace, String imageUrl, String createdAt) {}
