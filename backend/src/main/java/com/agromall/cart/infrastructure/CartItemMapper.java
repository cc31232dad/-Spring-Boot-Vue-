package com.agromall.cart.infrastructure;

import com.agromall.cart.domain.CartItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CartItemMapper extends BaseMapper<CartItem> {
    @Select("""
            <script>
            SELECT * FROM cart_items
            WHERE user_id = #{userId} AND id IN
            <foreach collection='itemIds' item='itemId' open='(' separator=',' close=')'>
                #{itemId}
            </foreach>
            FOR UPDATE
            </script>
            """)
    List<CartItem> selectOwnedItemsForUpdate(@Param("userId") Long userId,
                                             @Param("itemIds") List<Long> itemIds);
}
