package com.agromall.order.domain;

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
@TableName("orders")
public class Order {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private Long buyerId;
    private Long farmerId;
    private String orderType;
    private String status;
    private BigDecimal totalAmount;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static Order create(String orderNo, Long buyerId, Long farmerId, BigDecimal totalAmount,
                               String receiverName, String receiverPhone, String receiverAddress) {
        Order order = new Order();
        order.orderNo = orderNo;
        order.buyerId = buyerId;
        order.farmerId = farmerId;
        order.orderType = "NORMAL";
        order.status = OrderStatus.PENDING_SHIPMENT.name();
        order.totalAmount = totalAmount;
        order.receiverName = receiverName;
        order.receiverPhone = receiverPhone;
        order.receiverAddress = receiverAddress;
        return order;
    }

    public static Order createSeckill(String orderNo, Long buyerId, Long farmerId, BigDecimal totalAmount,
                                      String receiverName, String receiverPhone, String receiverAddress) {
        Order order = create(orderNo, buyerId, farmerId, totalAmount, receiverName, receiverPhone, receiverAddress);
        order.status = OrderStatus.PENDING_PAYMENT.name();
        order.orderType = "SECKILL";
        return order;
    }
}
