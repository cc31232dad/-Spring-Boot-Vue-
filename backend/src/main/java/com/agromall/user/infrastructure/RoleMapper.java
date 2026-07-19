package com.agromall.user.infrastructure;

import com.agromall.user.domain.Role;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Set;

@Mapper
public interface RoleMapper extends BaseMapper<Role> {

    @Select("""
            SELECT r.code
            FROM roles r
            INNER JOIN user_roles ur ON ur.role_id = r.id
            WHERE ur.user_id = #{userId}
            ORDER BY r.code
            """)
    Set<String> selectCodesByUserId(@Param("userId") Long userId);
}
