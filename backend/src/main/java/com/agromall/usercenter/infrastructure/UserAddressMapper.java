package com.agromall.usercenter.infrastructure;

import com.agromall.usercenter.domain.UserAddress;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserAddressMapper extends BaseMapper<UserAddress> {
    @Update("UPDATE user_addresses SET is_default = 0 WHERE user_id = #{userId} AND id <> #{addressId}")
    int clearDefaultExcept(@Param("userId") Long userId, @Param("addressId") Long addressId);
}
