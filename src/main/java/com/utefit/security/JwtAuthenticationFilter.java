package com.utefit.security;

import com.utefit.entity.User;
import com.utefit.enums.UserStatus;
import com.utefit.repository.UserRepository;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class JwtAuthenticationFilter
        extends OncePerRequestFilter {

    private final JwtService jwt;
    private final UserRepository users;
    private final CustomUserDetailsService detailsService;

    public JwtAuthenticationFilter(
            JwtService jwt,
            UserRepository users,
            CustomUserDetailsService detailsService
    ) {
        this.jwt = jwt;
        this.users = users;
        this.detailsService = detailsService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws ServletException, IOException {

        String token = null;

        // Lấy JWT từ cookie HttpOnly.
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {

                if ("UTEFIT_AUTH".equals(cookie.getName())) {
                    token = cookie.getValue();
                    break;
                }
            }
        }

        if (token != null
                && SecurityContextHolder
                .getContext()
                .getAuthentication() == null) {

            try {
                Claims claims = jwt.parseVerified(token);

                Long id = Long.parseLong(
                        claims.getSubject()
                );

                User user = users.findById(id).orElse(null);

                Integer tokenVersion =
                        claims.get("ver", Integer.class);

                if (user != null
                        && user.getStatus() == UserStatus.ACTIVE
                        && Boolean.TRUE.equals(user.getEnabled())
                        && tokenVersion != null
                        && tokenVersion.equals(
                        user.getTokenVersion() == null
                                ? 0
                                : user.getTokenVersion()
                )) {

                    UserDetails principal =
                            detailsService.loadUserByUsername(
                                    user.getEmail()
                            );

                    var authentication =
                            new UsernamePasswordAuthenticationToken(
                                    principal,
                                    null,
                                    principal.getAuthorities()
                            );

                    SecurityContextHolder
                            .getContext()
                            .setAuthentication(authentication);
                }

            } catch (
                    JwtException
                    | IllegalArgumentException
                    | org.springframework.security.core.userdetails
                            .UsernameNotFoundException ignored
            ) {
                // Token không hợp lệ, xem như Guest.
            }
        }

        chain.doFilter(request, response);
    }
}