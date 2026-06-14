package com.ecommerce.security;

import lombok.Getter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.List;

@Getter
public class UserPrincipal extends User {

    private final Long userId;
    private final String userEmail;

    public UserPrincipal(com.ecommerce.model.User user) {
        super(user.getEmail(), user.getPassword(),
              List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
        this.userId = user.getId();
        this.userEmail = user.getEmail();
    }
}
