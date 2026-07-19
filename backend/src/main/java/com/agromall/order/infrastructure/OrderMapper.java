package com.agromall.order.infrastructure;

import com.agromall.order.domain.Order;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {
    @Update("UPDATE orders SET status = 'CANCELLED' WHERE id = #{orderId} AND status = 'PENDING_SHIPMENT'")
    int markCancelledIfPending(@Param("orderId") Long orderId);
}
