package com.utefit.config;

import com.utefit.repository.UserRepository;
import com.utefit.security.CustomUserDetailsService;
import com.utefit.security.JwtAuthenticationFilter;
import com.utefit.security.JwtService;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder
    ) {
        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(userDetailsService);

        provider.setPasswordEncoder(passwordEncoder);

        return new ProviderManager(provider);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtService jwtService,
            UserRepository userRepository,
            CustomUserDetailsService userDetailsService
    ) throws Exception {

        JwtAuthenticationFilter jwtFilter =
                new JwtAuthenticationFilter(
                        jwtService,
                        userRepository,
                        userDetailsService
                );

        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(
                                new HttpSessionCsrfTokenRepository()
                        )
                        .ignoringRequestMatchers("/ws/**")
                )

                .authorizeHttpRequests(auth -> auth

                        .requestMatchers(
                                "/",
                                "/register",
                                "/verify-otp",
                                "/resend-otp",
                                "/login",
                                "/forgot-password",
                                "/reset-password",
                                "/access-denied",

                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/vendor/**",
                                "/error",
                                "/error/**",

                                // Guest
                                "/branches",
                                "/branches/**",
                                "/membership-plans",
                                "/membership-plans/**",
                                "/classes",
                                "/classes/**",

                                // Guest Review - Giai đoạn 6
                                "/reviews",

                                // Public Trainer
                                "/trainers",
                                "/trainers/**"
                        ).permitAll()

                        .requestMatchers(
                                "/notifications",
                                "/notifications/**"
                        )
                        .authenticated()

                        .requestMatchers("/member/**")
                        .hasRole("MEMBER")

                        .requestMatchers("/pt/**")
                        .hasRole("PT")

                        .requestMatchers("/admin/**")
                        .hasRole("ADMIN")

                        .requestMatchers("/ws", "/ws/**")
                        .authenticated()

                        .requestMatchers("/auth/logout")
                        .authenticated()

                        .anyRequest()
                        .denyAll()
                )

                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(
                                new LoginUrlAuthenticationEntryPoint(
                                        "/login"
                                )
                        )
                        .accessDeniedHandler(
                                (request, response, exception) ->
                                        response.sendError(
                                                HttpServletResponse.SC_FORBIDDEN
                                        )
                        )
                )

                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())

                .addFilterBefore(
                        jwtFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}