package com.agromall.seckill.domain;

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
@TableName("seckill_activity")
public class SeckillActivity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long productId;
    private Long farmerId;
    private BigDecimal seckillPrice;
    private Integer totalStock;
    private Integer limitPerUser;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static SeckillActivity draft(Long productId, Long farmerId, BigDecimal price, int stock,
                                        int limitPerUser, LocalDateTime startAt, LocalDateTime endAt) {
        SeckillActivity activity = new SeckillActivity();
        activity.productId = productId;
        activity.farmerId = farmerId;
        activity.seckillPrice = price;
        activity.totalStock = stock;
        activity.limitPerUser = limitPerUser;
        activity.startAt = startAt;
        activity.endAt = endAt;
        activity.status = SeckillActivityStatus.DRAFT.name();
        return activity;
    }
}
