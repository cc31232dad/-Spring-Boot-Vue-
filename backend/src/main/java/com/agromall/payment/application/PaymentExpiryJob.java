package com.agromall.payment.application;

import com.agromall.order.infrastructure.OrderMapper;
import com.agromall.payment.domain.PaymentRecord;
import com.agromall.payment.infrastructure.PaymentMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
public class PaymentExpiryJob {
    private final PaymentMapper paymentMapper;
    private final OrderMapper orderMapper;

    public PaymentExpiryJob(PaymentMapper paymentMapper, OrderMapper orderMapper) {
        this.paymentMapper = paymentMapper;
        this.orderMapper = orderMapper;
    }

    @Scheduled(fixedDelayString = "${agromall.payment.expiry-scan-ms:60000}")
    @Transactional
    public void closeExpiredPayments() {
        LocalDateTime now = LocalDateTime.now();
        for (PaymentRecord record : paymentMapper.selectExpired(now)) {
            if (paymentMapper.markClosed(record.getId(), now) == 1) {
                orderMapper.markCancelledIfAwaitingPayment(record.getOrderId());
            }
        }
    }
}
