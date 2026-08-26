package com.agromall.payment;

import com.agromall.common.exception.BusinessException;
import com.agromall.order.domain.Order;
import com.agromall.order.domain.OrderStatus;
import com.agromall.order.infrastructure.OrderMapper;
import com.agromall.payment.api.SandboxPaymentCallbackRequest;
import com.agromall.payment.application.PaymentService;
import com.agromall.payment.application.PaymentSignature;
import com.agromall.payment.domain.PaymentRecord;
import com.agromall.payment.infrastructure.PaymentMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PaymentServiceTest {

    @Test
    void reusesUnexpiredPendingPaymentForOwnedOrder() {
        PaymentMapper payments = mock(PaymentMapper.class);
        OrderMapper orders = mock(OrderMapper.class);
        PaymentSignature signature = mock(PaymentSignature.class);
        Order order = pendingOrder(9L, 3L);
        PaymentRecord record = PaymentRecord.pending(9L, order.getOrderNo(), 3L, order.getTotalAmount(), "PAY-1", LocalDateTime.now().plusMinutes(5));
        when(orders.selectOne(any())).thenReturn(order);
        when(payments.selectPendingByOrder(9L)).thenReturn(record);

        var result = new PaymentService(payments, orders, signature).create(3L, 9L);

        assertThat(result.paymentNo()).isEqualTo("PAY-1");
        verify(payments, never()).insert(any(PaymentRecord.class));
    }

    @Test
    void rejectsCallbackWithInvalidSignature() {
        PaymentMapper payments = mock(PaymentMapper.class);
        OrderMapper orders = mock(OrderMapper.class);
        PaymentSignature signature = mock(PaymentSignature.class);
        PaymentRecord record = PaymentRecord.pending(9L, "ORD-9", 3L, new BigDecimal("12.50"), "PAY-1", LocalDateTime.now().plusMinutes(5));
        when(payments.selectByPaymentNo("PAY-1")).thenReturn(record);
        when(signature.verify(any(), any(), any(), any(), anyLong(), any(), anyLong())).thenReturn(false);
        var request = new SandboxPaymentCallbackRequest("PAY-1", "ORD-9", new BigDecimal("12.50"), "SUCCESS", 1L, "bad");

        assertThatThrownBy(() -> new PaymentService(payments, orders, signature).callback(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Payment signature is invalid");
        verify(orders, never()).markPendingShipmentIfAwaitingPayment(any());
    }

    @Test
    void successfulCallbackPaysRecordAndMovesOrderToShipment() {
        PaymentMapper payments = mock(PaymentMapper.class);
        OrderMapper orders = mock(OrderMapper.class);
        PaymentSignature signature = mock(PaymentSignature.class);
        PaymentRecord pending = PaymentRecord.pending(9L, "ORD-9", 3L, new BigDecimal("12.50"), "PAY-1", LocalDateTime.now().plusMinutes(5));
        PaymentRecord paid = PaymentRecord.pending(9L, "ORD-9", 3L, new BigDecimal("12.50"), "PAY-1", LocalDateTime.now().plusMinutes(5));
        paid.setStatus("PAID");
        when(payments.selectByPaymentNo("PAY-1")).thenReturn(pending, paid);
        when(signature.verify(any(), any(), any(), any(), anyLong(), any(), anyLong())).thenReturn(true);
        when(orders.markPendingShipmentIfAwaitingPayment(9L)).thenReturn(1);
        var request = new SandboxPaymentCallbackRequest("PAY-1", "ORD-9", new BigDecimal("12.50"), "SUCCESS", 1L, "ok");

        var result = new PaymentService(payments, orders, signature).callback(request);

        assertThat(result.status()).isEqualTo("PAID");
        verify(payments).markPaid(eq(pending.getId()), any());
    }

    private Order pendingOrder(Long id, Long buyerId) {
        Order order = Order.create("ORD-9", buyerId, 4L, new BigDecimal("12.50"), "Buyer", "13800000000", "Address");
        order.setId(id);
        order.setStatus(OrderStatus.PENDING_PAYMENT.name());
        return order;
    }
}
