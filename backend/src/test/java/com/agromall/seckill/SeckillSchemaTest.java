package com.agromall.seckill;

import com.agromall.order.domain.Order;
import com.agromall.order.infrastructure.OrderMapper;
import com.agromall.product.domain.Product;
import com.agromall.product.infrastructure.ProductMapper;
import com.agromall.seckill.domain.SeckillActivity;
import com.agromall.seckill.domain.SeckillActivityStatus;
import com.agromall.seckill.domain.SeckillOrder;
import com.agromall.user.domain.User;
import com.agromall.user.infrastructure.UserMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class SeckillSchemaTest {

    @Autowired private ProductMapper productMapper;
    @Autowired private OrderMapper orderMapper;
    @Autowired private UserMapper userMapper;

    @Test
    void migrationSupportsSeckillActivityAndOrderType() {
        User farmer = User.create("seckillschemafarmer", "hash", "13900000901");
        userMapper.insert(farmer);
        Product product = Product.create(1L, farmer.getId(), "Schema seckill product", "Fresh", new BigDecimal("10.00"),
                20, "Shaanxi", "https://example.com/seckill.jpg");
        productMapper.insert(product);

        SeckillActivity activity = SeckillActivity.draft(product.getId(), farmer.getId(), new BigDecimal("5.00"), 10, 1,
                LocalDateTime.now().plusMinutes(1), LocalDateTime.now().plusHours(1));
        assertThat(activity.getStatus()).isEqualTo(SeckillActivityStatus.DRAFT.name());
        assertThat(activity.getTotalStock()).isEqualTo(10);

        User buyer = User.create("seckillschemabuyer", "hash", "13900000902");
        userMapper.insert(buyer);
        Order order = Order.create("SECKILL-SCHEMA-001", buyer.getId(), farmer.getId(), new BigDecimal("5.00"), "Buyer", "13800000000", "Address");
        order.setOrderType("SECKILL");
        orderMapper.insert(order);
        assertThat(orderMapper.selectById(order.getId()).getOrderType()).isEqualTo("SECKILL");
        assertThat(orderMapper.selectList(Wrappers.<Order>lambdaQuery().eq(Order::getOrderType, "SECKILL"))).hasSize(1);
        assertThat(SeckillOrder.create(1L, order.getId(), buyer.getId()).getQuantity()).isOne();
    }
}
