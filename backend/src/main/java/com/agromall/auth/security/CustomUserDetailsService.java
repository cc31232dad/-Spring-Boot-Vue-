package com.agromall.auth.security;

import com.agromall.user.domain.User;
import com.agromall.user.infrastructure.RoleMapper;
import com.agromall.user.infrastructure.UserMapper;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;

    public CustomUserDetailsService(UserMapper userMapper, RoleMapper roleMapper) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userMapper.selectByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return toPrincipal(user, roleMapper.selectCodesByUserId(user.getId()));
    }

    public AuthenticatedUser toPrincipal(User user, Set<String> roles) {
        return new AuthenticatedUser(
                user.getId(),
                user.getUsername(),
                user.getPasswordHash(),
                Boolean.TRUE.equals(user.getEnabled()),
                roles
        );
    }

    public record AuthenticatedUser(
            Long userId,
            String username,
            String password,
            boolean enabled,
            Set<String> roles
    ) implements UserDetails {

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return roles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .collect(Collectors.toSet());
        }

        @Override
        public String getPassword() {
            return password;
        }

        @Override
        public String getUsername() {
            return username;
        }

        @Override
        public boolean isEnabled() {
            return enabled;
        }
    }
}
