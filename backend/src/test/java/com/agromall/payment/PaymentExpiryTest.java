package com.agromall.payment;

import com.agromall.order.infrastructure.OrderMapper;
import com.agromall.payment.application.PaymentExpiryJob;
import com.agromall.payment.domain.PaymentRecord;
import com.agromall.payment.infrastructure.PaymentMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PaymentExpiryTest {
    @Test
    void closesExpiredPaymentAndCancelsStillPendingOrder() {
        PaymentMapper payments = mock(PaymentMapper.class);
        OrderMapper orders = mock(OrderMapper.class);
        PaymentRecord record = PaymentRecord.pending(9L, "ORD-9", 3L, new BigDecimal("12.50"), "PAY-1", LocalDateTime.now().minusMinutes(1));
        when(payments.selectExpired(any())).thenReturn(List.of(record));
        when(payments.markClosed(eq(record.getId()), any())).thenReturn(1);

        new PaymentExpiryJob(payments, orders).closeExpiredPayments();

        verify(orders).markCancelledIfAwaitingPayment(9L);
    }
}
