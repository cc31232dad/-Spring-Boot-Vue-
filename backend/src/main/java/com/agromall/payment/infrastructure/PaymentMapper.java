package com.agromall.payment.infrastructure;

import com.agromall.payment.domain.PaymentRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface PaymentMapper extends BaseMapper<PaymentRecord> {
    @Select("SELECT * FROM payment_records WHERE payment_no = #{paymentNo}")
    PaymentRecord selectByPaymentNo(@Param("paymentNo") String paymentNo);

    @Select("SELECT * FROM payment_records WHERE order_id = #{orderId} AND status = 'PENDING' ORDER BY id DESC LIMIT 1")
    PaymentRecord selectPendingByOrder(@Param("orderId") Long orderId);

    @Select("SELECT * FROM payment_records WHERE status = 'PENDING' AND expires_at <= #{now} ORDER BY id LIMIT 100")
    List<PaymentRecord> selectExpired(@Param("now") LocalDateTime now);

    @Update("UPDATE payment_records SET status = 'PAID', paid_at = #{paidAt}, callback_count = callback_count + 1, last_callback_at = #{paidAt} WHERE id = #{id} AND status = 'PENDING'")
    int markPaid(@Param("id") Long id, @Param("paidAt") LocalDateTime paidAt);

    @Update("UPDATE payment_records SET status = 'FAILED', callback_count = callback_count + 1, last_callback_at = #{at} WHERE id = #{id} AND status = 'PENDING'")
    int markFailed(@Param("id") Long id, @Param("at") LocalDateTime at);

    @Update("UPDATE payment_records SET status = 'CLOSED', callback_count = callback_count + 1, last_callback_at = #{at} WHERE id = #{id} AND status = 'PENDING'")
    int markClosed(@Param("id") Long id, @Param("at") LocalDateTime at);

    @Update("UPDATE payment_records SET callback_count = callback_count + 1, last_callback_at = #{at} WHERE id = #{id}")
    int recordCallback(@Param("id") Long id, @Param("at") LocalDateTime at);
}
