package com.agromall.seckill.infrastructure;

import com.agromall.seckill.domain.SeckillOrder;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SeckillOrderMapper extends BaseMapper<SeckillOrder> {
}
