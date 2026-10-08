package com.example.delivery.global.security;

import com.example.delivery.user.entity.User;
import com.example.delivery.user.entity.UserRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class CustomUserDetails implements UserDetails {

    private final Long userId;
    private final String loginId;
    private final String password;
    private final UserRole role;
    private final boolean enabled;

    public CustomUserDetails(User user) {
        this.userId = user.getId();
        this.loginId = user.getLoginId();
        this.password = user.getPassword();
        this.role = user.getRole();
        this.enabled = !user.isDeleted();
    }

    public Long getUserId() {
        return userId;
    }

    public UserRole getRole() {
        return role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return loginId;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

}
