package com.agromall.auth.application;

import com.agromall.auth.api.RegisterRequest;
import com.agromall.auth.api.UserSessionView;
import com.agromall.common.exception.BusinessException;
import com.agromall.common.exception.ErrorCode;
import com.agromall.user.domain.Role;
import com.agromall.user.domain.User;
import com.agromall.user.infrastructure.RoleMapper;
import com.agromall.user.infrastructure.UserMapper;
import com.agromall.user.infrastructure.UserRoleMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final String USER_ROLE = "USER";

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(UserMapper userMapper, RoleMapper roleMapper, UserRoleMapper userRoleMapper) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
    }

    @Transactional
    public UserSessionView register(RegisterRequest request) {
        if (userMapper.selectByUsername(request.username()).isPresent()
                || userMapper.selectOne(Wrappers.<User>lambdaQuery()
                .eq(User::getPhone, request.phone())) != null) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS);
        }

        User user = User.create(request.username(), passwordEncoder.encode(request.password()), request.phone());
        userMapper.insert(user);

        Role userRole = roleMapper.selectOne(Wrappers.<Role>lambdaQuery()
                .eq(Role::getCode, USER_ROLE));
        userRoleMapper.insert(user.getId(), userRole.getId());

        return new UserSessionView(user.getId(), user.getUsername(), roleMapper.selectCodesByUserId(user.getId()));
    }
}
