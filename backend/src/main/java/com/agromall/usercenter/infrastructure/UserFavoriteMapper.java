package com.agromall.usercenter.infrastructure;

import com.agromall.usercenter.domain.UserFavorite;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserFavoriteMapper extends BaseMapper<UserFavorite> {
    @Select("SELECT f.id, f.user_id, f.product_id, f.created_at, f.updated_at, "
            + "p.name AS product_name, p.price AS product_price, p.stock AS product_stock, "
            + "p.origin_place AS product_origin_place, p.image_url AS product_image_url "
            + "FROM user_favorites f JOIN product p ON p.id = f.product_id "
            + "WHERE f.user_id = #{userId} ORDER BY f.created_at DESC")
    List<FavoriteRow> selectRowsByUserId(@Param("userId") Long userId);

    record FavoriteRow(Long id, Long userId, Long productId, java.time.LocalDateTime createdAt,
                       java.time.LocalDateTime updatedAt, String productName,
                       java.math.BigDecimal productPrice, Integer productStock,
                       String productOriginPlace, String productImageUrl) {}
}
