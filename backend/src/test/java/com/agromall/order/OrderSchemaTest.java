package com.agromall.order;

import com.agromall.cart.domain.CartItem;
import com.agromall.cart.infrastructure.CartItemMapper;
import com.agromall.order.domain.Order;
import com.agromall.order.domain.OrderItem;
import com.agromall.order.domain.OrderStatus;
import com.agromall.order.infrastructure.OrderItemMapper;
import com.agromall.order.infrastructure.OrderMapper;
import com.agromall.product.domain.Product;
import com.agromall.product.infrastructure.ProductMapper;
import com.agromall.user.domain.User;
import com.agromall.user.infrastructure.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class OrderSchemaTest {

    @Autowired CartItemMapper cartItemMapper;
    @Autowired OrderMapper orderMapper;
    @Autowired OrderItemMapper orderItemMapper;
    @Autowired ProductMapper productMapper;
    @Autowired UserMapper userMapper;

    @Test
    void createsCartOrderAndOrderItemRows() {
        User farmer = User.create("orderschemafarmer", "hash", "13900000002");
        userMapper.insert(farmer);

        Product product = Product.create(1L, farmer.getId(), "订单测试苹果", "香甜苹果",
                new BigDecimal("12.50"), 20, "陕西洛川", "https://example.com/apple.jpg");
        productMapper.insert(product);

        CartItem cartItem = CartItem.create(200L, product.getId(), 2);
        cartItemMapper.insert(cartItem);

        Order order = Order.create("ORD202607190001", 200L, farmer.getId(),
                new BigDecimal("25.00"), "张三", "13800000000", "陕西省西安市");
        orderMapper.insert(order);

        OrderItem item = OrderItem.create(order.getId(), product.getId(), "订单测试苹果",
                "https://example.com/apple.jpg", "陕西洛川", new BigDecimal("12.50"), 2);
        orderItemMapper.insert(item);

        assertThat(cartItemMapper.selectById(cartItem.getId()).getQuantity()).isEqualTo(2);
        assertThat(orderMapper.selectById(order.getId()).getStatus()).isEqualTo(OrderStatus.PENDING_SHIPMENT.name());
        assertThat(orderItemMapper.selectById(item.getId()).getSubtotal()).isEqualByComparingTo("25.00");
    }
}
