package com.agromall.seckill.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@TableName("seckill_orders")
public class SeckillOrder {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long activityId;
    private Long orderId;
    private Long buyerId;
    private Integer quantity;
    private LocalDateTime createdAt;

    public static SeckillOrder create(Long activityId, Long orderId, Long buyerId) {
        SeckillOrder result = new SeckillOrder();
        result.activityId = activityId;
        result.orderId = orderId;
        result.buyerId = buyerId;
        result.quantity = 1;
        return result;
    }
}
