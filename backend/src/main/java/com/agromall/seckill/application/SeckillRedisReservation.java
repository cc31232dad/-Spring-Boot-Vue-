package com.agromall.seckill.application;

import com.agromall.common.exception.BusinessException;
import com.agromall.common.exception.ErrorCode;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SeckillRedisReservation {

    private static final DefaultRedisScript<Long> RESERVE_SCRIPT = reserveScript();
    private final StringRedisTemplate redis;

    public SeckillRedisReservation(StringRedisTemplate redis) {
        this.redis = redis;
    }

    private static DefaultRedisScript<Long> reserveScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("scripts/seckill_reserve.lua"));
        script.setResultType(Long.class);
        return script;
    }

    public void warmStock(Long activityId, int stock) {
        redis.opsForValue().set(stockKey(activityId), Integer.toString(stock));
        redis.delete(buyersKey(activityId));
    }

    public void reserve(Long activityId, Long userId) {
        Long result = redis.execute(RESERVE_SCRIPT, List.of(stockKey(activityId), buyersKey(activityId)),
                userId.toString());
        if (result == null) {
            throw new BusinessException(ErrorCode.SECKILL_ORDER_FAILED);
        }
        switch (result.intValue()) {
            case 0 -> { }
            case 1 -> throw new BusinessException(ErrorCode.SECKILL_SOLD_OUT);
            case 2 -> throw new BusinessException(ErrorCode.SECKILL_ALREADY_BOUGHT);
            default -> throw new BusinessException(ErrorCode.SECKILL_ORDER_FAILED);
        }
    }

    public void compensate(Long activityId, Long userId) {
        redis.opsForValue().increment(stockKey(activityId));
        redis.opsForSet().remove(buyersKey(activityId), userId.toString());
    }

    public String stockKey(Long activityId) {
        return "seckill:stock:" + activityId;
    }

    public String buyersKey(Long activityId) {
        return "seckill:buyers:" + activityId;
    }
}
