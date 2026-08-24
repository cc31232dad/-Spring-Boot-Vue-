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
@TableName("order_items")
public class OrderItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private Long productId;
    private String productName;
    private String productImageUrl;
    private String originPlace;
    private BigDecimal unitPrice;
    private Integer quantity;
    private BigDecimal subtotal;
    private LocalDateTime createdAt;

    public static OrderItem create(Long orderId, Long productId, String productName, String productImageUrl,
                                   String originPlace, BigDecimal unitPrice, Integer quantity) {
        OrderItem orderItem = new OrderItem();
        orderItem.orderId = orderId;
        orderItem.productId = productId;
        orderItem.productName = productName;
        orderItem.productImageUrl = productImageUrl;
        orderItem.originPlace = originPlace;
        orderItem.unitPrice = unitPrice;
        orderItem.quantity = quantity;
        orderItem.subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
        return orderItem;
    }
}
