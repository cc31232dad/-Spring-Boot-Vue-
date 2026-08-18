package com.agromall.seckill.infrastructure;

import com.agromall.seckill.domain.SeckillActivity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SeckillActivityMapper extends BaseMapper<SeckillActivity> {
    @Update("UPDATE seckill_activity SET status = 'PUBLISHED' WHERE id = #{activityId} AND status = 'DRAFT'")
    int markPublished(@Param("activityId") Long activityId);

    @Update("UPDATE seckill_activity SET status = 'ENDED' WHERE id = #{activityId} AND status = 'PUBLISHED'")
    int markEnded(@Param("activityId") Long activityId);
}
