package com.agromall.seckill;

import com.agromall.common.exception.BusinessException;
import com.agromall.seckill.application.SeckillRedisReservation;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SeckillRedisReservationTest {

    @SuppressWarnings("unchecked")
    @Test
    void mapsLuaSuccessAndFailureCodes() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.execute(any(DefaultRedisScript.class), anyList(), eq("42"))).thenReturn(0L);
        SeckillRedisReservation reservation = new SeckillRedisReservation(redis);

        reservation.reserve(7L, 42L);

        when(redis.execute(any(DefaultRedisScript.class), anyList(), eq("42"))).thenReturn(1L);
        assertThatThrownBy(() -> reservation.reserve(7L, 42L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Seckill activity is sold out");

        when(redis.execute(any(DefaultRedisScript.class), anyList(), eq("42"))).thenReturn(2L);
        assertThatThrownBy(() -> reservation.reserve(7L, 42L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("User already bought this seckill item");
    }

    @Test
    void compensationRestoresStockAndRemovesBuyer() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        SetOperations<String, String> sets = mock(SetOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(redis.opsForSet()).thenReturn(sets);
        SeckillRedisReservation reservation = new SeckillRedisReservation(redis);

        reservation.compensate(7L, 42L);

        verify(values).increment("seckill:stock:7");
        verify(sets).remove("seckill:buyers:7", "42");
    }

    @Test
    void prefixesEverySeckillKeyWithEnvironmentNamespace() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        SeckillRedisReservation reservation = new SeckillRedisReservation(redis, "agromall:test:");

        assertThat(reservation.stockKey(7L)).isEqualTo("agromall:test:seckill:stock:7");
        assertThat(reservation.buyersKey(7L)).isEqualTo("agromall:test:seckill:buyers:7");
    }
}
