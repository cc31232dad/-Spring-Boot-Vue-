package com.agromall.user.infrastructure;

import com.agromall.user.domain.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.apache.ibatis.annotations.Mapper;

import java.util.Optional;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    default Optional<User> selectByUsername(String username) {
        return Optional.ofNullable(selectOne(Wrappers.<User>lambdaQuery()
                .eq(User::getUsername, username)));
    }
}
