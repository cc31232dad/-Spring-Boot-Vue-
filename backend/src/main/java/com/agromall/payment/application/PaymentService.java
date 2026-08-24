package com.agromall.payment.application;

import com.agromall.common.exception.BusinessException;
import com.agromall.common.exception.ErrorCode;
import com.agromall.order.domain.Order;
import com.agromall.order.domain.OrderStatus;
import com.agromall.order.infrastructure.OrderMapper;
import com.agromall.payment.api.PaymentView;
import com.agromall.payment.api.SandboxPaymentCallbackRequest;
import com.agromall.payment.api.SandboxPaymentSimulationRequest;
import com.agromall.payment.domain.PaymentRecord;
import com.agromall.payment.domain.PaymentStatus;
import com.agromall.payment.infrastructure.PaymentMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class PaymentService {
    private static final int EXPIRY_MINUTES = 15;
    private final PaymentMapper paymentMapper;
    private final OrderMapper orderMapper;
    private final PaymentSignature signature;

    public PaymentService(PaymentMapper paymentMapper, OrderMapper orderMapper, PaymentSignature signature) {
        this.paymentMapper = paymentMapper;
        this.orderMapper = orderMapper;
        this.signature = signature;
    }

    @Transactional
    public PaymentView create(Long buyerId, Long orderId) {
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getId, orderId).eq(Order::getBuyerId, buyerId));
        if (order == null) throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        if (!OrderStatus.PENDING_PAYMENT.name().equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.PAYMENT_INVALID_STATUS);
        }
        LocalDateTime now = LocalDateTime.now();
        PaymentRecord existing = paymentMapper.selectPendingByOrder(orderId);
        if (existing != null && existing.getExpiresAt().isAfter(now)) return view(existing);
        if (existing != null) paymentMapper.markClosed(existing.getId(), now);
        PaymentRecord record = PaymentRecord.pending(order.getId(), order.getOrderNo(), buyerId, order.getTotalAmount(),
                nextPaymentNo(), now.plusMinutes(EXPIRY_MINUTES));
        paymentMapper.insert(record);
        return view(record);
    }

    @Transactional
    public PaymentView callback(SandboxPaymentCallbackRequest request) {
        PaymentRecord record = paymentMapper.selectByPaymentNo(request.paymentNo());
        if (record == null) throw new BusinessException(ErrorCode.PAYMENT_NOT_FOUND);
        if (!record.getOrderNo().equals(request.orderNo())) throw new BusinessException(ErrorCode.PAYMENT_NOT_FOUND);
        if (record.getAmount().compareTo(request.amount()) != 0) throw new BusinessException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        LocalDateTime now = LocalDateTime.now();
        long nowSeconds = Instant.now().getEpochSecond();
        if (!signature.verify(request.paymentNo(), request.orderNo(), request.amount(), request.result(), request.timestamp(),
                request.signature(), nowSeconds)) throw new BusinessException(ErrorCode.PAYMENT_SIGNATURE_INVALID);
        if (!PaymentStatus.PENDING.name().equals(record.getStatus())) {
            paymentMapper.recordCallback(record.getId(), now);
            return view(record);
        }
        if (record.getExpiresAt().isBefore(now)) {
            paymentMapper.markClosed(record.getId(), now);
            throw new BusinessException(ErrorCode.PAYMENT_EXPIRED);
        }
        if ("FAILURE".equalsIgnoreCase(request.result())) {
            paymentMapper.markFailed(record.getId(), now);
            return view(paymentMapper.selectByPaymentNo(record.getPaymentNo()));
        }
        if (!"SUCCESS".equalsIgnoreCase(request.result())) throw new BusinessException(ErrorCode.PAYMENT_INVALID_STATUS);
        if (orderMapper.markPendingShipmentIfAwaitingPayment(record.getOrderId()) != 1) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS);
        }
        paymentMapper.markPaid(record.getId(), now);
        return view(paymentMapper.selectByPaymentNo(record.getPaymentNo()));
    }

    public PaymentView simulate(Long buyerId, SandboxPaymentSimulationRequest request) {
        PaymentRecord record = paymentMapper.selectByPaymentNo(request.paymentNo());
        if (record == null || !record.getBuyerId().equals(buyerId)) throw new BusinessException(ErrorCode.PAYMENT_NOT_FOUND);
        long timestamp = Instant.now().getEpochSecond();
        SandboxPaymentCallbackRequest callback = new SandboxPaymentCallbackRequest(record.getPaymentNo(), record.getOrderNo(),
                record.getAmount(), request.result(), timestamp,
                signature.sign(record.getPaymentNo(), record.getOrderNo(), record.getAmount(), request.result(), timestamp));
        return callback(callback);
    }

    private PaymentView view(PaymentRecord record) {
        return new PaymentView(record.getPaymentNo(), record.getOrderNo(), record.getAmount(), record.getChannel(),
                record.getStatus(), record.getExpiresAt(), record.getPaidAt());
    }

    private String nextPaymentNo() {
        return "PAY" + System.currentTimeMillis() + "%06d".formatted(ThreadLocalRandom.current().nextInt(1_000_000));
    }
}
