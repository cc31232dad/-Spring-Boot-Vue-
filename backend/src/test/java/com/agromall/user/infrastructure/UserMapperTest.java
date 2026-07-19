package com.agromall.user.infrastructure;

import com.agromall.user.domain.Role;
import com.agromall.user.domain.User;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class UserMapperTest {

    @Autowired
    private UserMapper users;

    @Autowired
    private RoleMapper roles;

    @Autowired
    private UserRoleMapper userRoles;

    @Test
    void findsUserByUsername() {
        User user = User.create("alice", "hash", "13800000000");
        users.insert(user);

        assertThat(users.selectByUsername("alice"))
                .isPresent()
                .get()
                .extracting(User::getUsername)
                .isEqualTo("alice");
    }

    @Test
    void seedsFixedRoles() {
        Set<String> codes = Set.of(
                roles.selectOne(Wrappers.<Role>lambdaQuery().eq(Role::getCode, "USER")).getCode(),
                roles.selectOne(Wrappers.<Role>lambdaQuery().eq(Role::getCode, "FARMER")).getCode(),
                roles.selectOne(Wrappers.<Role>lambdaQuery().eq(Role::getCode, "ADMIN")).getCode()
        );

        assertThat(codes).containsExactlyInAnyOrder("USER", "FARMER", "ADMIN");
    }

    @Test
    void findsRoleCodesAssignedToUser() {
        User user = User.create("bob", "hash", "13900000000");
        users.insert(user);
        Role farmer = roles.selectOne(Wrappers.<Role>lambdaQuery().eq(Role::getCode, "FARMER"));
        userRoles.insert(user.getId(), farmer.getId());

        assertThat(roles.selectCodesByUserId(user.getId())).containsExactly("FARMER");
    }
}
