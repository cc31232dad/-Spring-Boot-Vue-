package com.agromall.payment.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@TableName("payment_records")
public class PaymentRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String paymentNo;
    private Long orderId;
    private String orderNo;
    private Long buyerId;
    private BigDecimal amount;
    private String channel;
    private String status;
    private LocalDateTime expiresAt;
    private LocalDateTime paidAt;
    private Integer callbackCount;
    private LocalDateTime lastCallbackAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static PaymentRecord pending(Long orderId, String orderNo, Long buyerId, BigDecimal amount,
                                        String paymentNo, LocalDateTime expiresAt) {
        PaymentRecord record = new PaymentRecord();
        record.paymentNo = paymentNo;
        record.orderId = orderId;
        record.orderNo = orderNo;
        record.buyerId = buyerId;
        record.amount = amount;
        record.channel = "SANDBOX";
        record.status = PaymentStatus.PENDING.name();
        record.expiresAt = expiresAt;
        record.callbackCount = 0;
        return record;
    }
}
