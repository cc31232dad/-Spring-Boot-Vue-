package com.agromall.order.api;

import java.math.BigDecimal;
import java.util.List;

public record OrderView(Long id, String orderNo, Long buyerId, Long farmerId, String status,
                        String orderType, BigDecimal totalAmount, String receiverName, String receiverPhone,
                        String receiverAddress, List<OrderItemView> items) {}
