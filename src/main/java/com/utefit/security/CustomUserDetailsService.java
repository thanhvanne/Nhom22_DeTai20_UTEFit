package com.utefit.security;

import com.utefit.entity.User;
import com.utefit.enums.UserStatus;
import com.utefit.repository.UserRepository;

import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class CustomUserDetailsService
        implements UserDetailsService {

    private final UserRepository users;

    public CustomUserDetailsService(UserRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        String normalized =
                email.trim().toLowerCase(Locale.ROOT);

        User user = users.findByEmail(normalized)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "Không tìm thấy tài khoản"
                        )
                );

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())
                .roles(user.getRole().name())
                .disabled(
                        !Boolean.TRUE.equals(user.getEnabled())
                                || user.getStatus() != UserStatus.ACTIVE
                )
                .accountLocked(
                        user.getStatus() == UserStatus.LOCKED
                )
                .build();
    }
}