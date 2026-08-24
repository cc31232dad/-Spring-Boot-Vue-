package com.agromall.payment.api;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentView(String paymentNo, String orderNo, BigDecimal amount, String channel, String status,
                          LocalDateTime expiresAt, LocalDateTime paidAt) {}
