package com.agromall.cart.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@TableName("cart_items")
public class CartItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long productId;
    private Integer quantity;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CartItem create(Long userId, Long productId, Integer quantity) {
        CartItem cartItem = new CartItem();
        cartItem.userId = userId;
        cartItem.productId = productId;
        cartItem.quantity = quantity;
        return cartItem;
    }
}
