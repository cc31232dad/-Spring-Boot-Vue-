package com.agromall.seckill.infrastructure;

import com.agromall.seckill.domain.SeckillOrder;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SeckillOrderMapper extends BaseMapper<SeckillOrder> {
    @Select("SELECT so.* FROM seckill_orders so JOIN orders o ON o.id = so.order_id "
            + "WHERE o.status = 'PENDING_PAYMENT' AND o.created_at < #{cutoff}")
    List<SeckillOrder> selectExpired(java.time.LocalDateTime cutoff);
}
