package com.agromall.auth.application;

import com.agromall.auth.api.RegisterRequest;
import com.agromall.auth.api.LoginRequest;
import com.agromall.auth.api.TokenView;
import com.agromall.auth.api.UserSessionView;
import com.agromall.auth.security.JwtService;
import com.agromall.common.exception.BusinessException;
import com.agromall.common.exception.ErrorCode;
import com.agromall.user.domain.Role;
import com.agromall.user.domain.User;
import com.agromall.user.infrastructure.RoleMapper;
import com.agromall.user.infrastructure.UserMapper;
import com.agromall.user.infrastructure.UserRoleMapper;
import com.agromall.farmer.domain.FarmerApplicationStatus;
import com.agromall.farmer.infrastructure.FarmerProfileMapper;
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
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final FarmerProfileMapper farmerProfileMapper;

    public AuthService(
            UserMapper userMapper,
            RoleMapper roleMapper,
            UserRoleMapper userRoleMapper,
            BCryptPasswordEncoder passwordEncoder,
            JwtService jwtService,
            FarmerProfileMapper farmerProfileMapper
    ) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.farmerProfileMapper = farmerProfileMapper;
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

    public TokenView login(LoginRequest request) {
        User user = userMapper.selectByUsernameOrPhone(request.username())
                .filter(candidate -> passwordEncoder.matches(request.password(), candidate.getPasswordHash()))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        var roles = roleMapper.selectCodesByUserId(user.getId());
        if (request.role() != null && !request.role().isBlank() && !roles.contains(request.role()))
            throw new BusinessException(ErrorCode.FORBIDDEN);
        if (Boolean.FALSE.equals(user.getEnabled())) throw new BusinessException(ErrorCode.FORBIDDEN);
        if (roles.contains("FARMER")) {
            var profile = farmerProfileMapper.selectOne(Wrappers.<com.agromall.farmer.domain.FarmerProfile>lambdaQuery()
                    .eq(com.agromall.farmer.domain.FarmerProfile::getUserId, user.getId()));
            if (profile != null && profile.getStatus() == FarmerApplicationStatus.PENDING)
                throw new BusinessException(ErrorCode.FARMER_APPLICATION_PENDING);
            if (profile != null && profile.getStatus() == FarmerApplicationStatus.REJECTED)
                throw new BusinessException(ErrorCode.FARMER_APPLICATION_REJECTED);
        }

        return TokenView.bearer(jwtService.issue(user, roles));
    }
}
