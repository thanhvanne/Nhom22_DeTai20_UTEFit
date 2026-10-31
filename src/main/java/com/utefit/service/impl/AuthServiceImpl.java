package com.utefit.service.impl;

import com.utefit.dto.auth.*;
import com.utefit.entity.User;
import com.utefit.enums.*;
import com.utefit.repository.UserRepository;
import com.utefit.service.AuthService;
import com.utefit.service.OtpService;
import com.utefit.exception.InvalidOtpException;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final OtpService otpService;
    private final AuthenticationManager authenticationManager;

    public AuthServiceImpl(
            UserRepository users,
            PasswordEncoder encoder,
            OtpService otpService,
            AuthenticationManager authenticationManager
    ) {
        this.users = users;
        this.encoder = encoder;
        this.otpService = otpService;
        this.authenticationManager = authenticationManager;
    }

    private String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    @Override
    @Transactional
    public String register(RegisterRequest request) {
        com.utefit.validation.FormChecks.validate(request);

        if (!request.getPassword()
                .equals(request.getConfirmPassword())) {

            throw new IllegalArgumentException(
                    "Mật khẩu xác nhận không khớp."
            );
        }

        String email = normalize(request.getEmail());

        if (users.existsByEmail(email)) {
            throw new IllegalArgumentException(
                    "Email đã tồn tại."
            );
        }

        User user = new User();

        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName().trim());
        user.setEmail(email);
        user.setPhone(request.getPhone());

        // BCrypt
        user.setPassword(
                encoder.encode(request.getPassword())
        );

        // Guest chỉ được đăng ký MEMBER.
        user.setRole(UserRole.MEMBER);

        user.setStatus(UserStatus.UNVERIFIED);
        user.setEnabled(false);
        user.setTokenVersion(0);

        users.saveAndFlush(user);

        // Gửi OTP đăng ký.
        otpService.issueAndSend(
                user,
                OtpType.REGISTER
        );

        return email;
    }


    @Override
    @Transactional(
            noRollbackFor = InvalidOtpException.class
    )
    public void verifyRegistration(
            VerifyOtpRequest request
    ) {
        com.utefit.validation.FormChecks.validate(request);
        User user = users
                .findByEmail(normalize(request.getEmail()))
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Thông tin xác thực không hợp lệ."
                        )
                );

        if (user.getStatus() != UserStatus.UNVERIFIED) {
            throw new IllegalArgumentException(
                    "Tài khoản đã xác thực hoặc bị khóa."
            );
        }

        boolean valid = otpService.consume(
                user,
                OtpType.REGISTER,
                request.getCode()
        );

        if (!valid) {
            throw new InvalidOtpException(
                    "OTP sai, hết hạn hoặc đã sử dụng."
            );
        }

        user.setStatus(UserStatus.ACTIVE);
        user.setEnabled(true);

        users.save(user);
    }


    @Override
    public void resendRegistrationOtp(String email) {
        com.utefit.validation.InputRules.require("email",email);

        User user = users
                .findByEmail(normalize(email))
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Không thể gửi OTP."
                        )
                );

        if (user.getStatus() != UserStatus.UNVERIFIED) {
            throw new IllegalArgumentException(
                    "Tài khoản không chờ xác thực."
            );
        }

        otpService.issueAndSend(
                user,
                OtpType.REGISTER
        );
    }

    @Override
    public User authenticate(
            String email,
            String rawPassword
    ) {
        com.utefit.validation.InputRules.require("email",email);
        String normalized = normalize(email);

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        normalized,
                        rawPassword
                )
        );

        return users
                .findByEmail(normalized)
                .orElseThrow();
    }

    @Override
    public void requestPasswordReset(String email) {
        com.utefit.validation.InputRules.require("email",email);

        users.findByEmail(normalize(email))
                .ifPresent(user -> {

                    if (user.getStatus() == UserStatus.ACTIVE
                            && Boolean.TRUE.equals(
                            user.getEnabled())) {

                        otpService.issueAndSend(
                                user,
                                OtpType.FORGOT_PASSWORD
                        );
                    }
                });
    }


    @Override
    @Transactional(
            noRollbackFor = InvalidOtpException.class
    )
    public void resetPassword(
            ResetPasswordRequest request
    ) {
        com.utefit.validation.FormChecks.validate(request);
        if (!request.getNewPassword()
                .equals(request.getConfirmPassword())) {

            throw new IllegalArgumentException(
                    "Mật khẩu xác nhận không khớp."
            );
        }

        User user = users
                .findByEmail(normalize(request.getEmail()))
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "OTP không hợp lệ."
                        )
                );

        if (user.getStatus() != UserStatus.ACTIVE
                || !Boolean.TRUE.equals(user.getEnabled())) {

            throw new IllegalArgumentException(
                    "Tài khoản không được phép đổi mật khẩu."
            );
        }

        boolean valid = otpService.consume(
                user,
                OtpType.FORGOT_PASSWORD,
                request.getCode()
        );

        if (!valid) {
            throw new InvalidOtpException(
                    "OTP sai, hết hạn hoặc đã sử dụng."
            );
        }

        user.setPassword(
                encoder.encode(request.getNewPassword())
        );

        int oldVersion = user.getTokenVersion() == null
                ? 0
                : user.getTokenVersion();

        user.setTokenVersion(oldVersion + 1);

        users.save(user);
    }


    @Override
    @Transactional
    public void revokeTokensForUser(String email) {

        users.findByEmail(normalize(email))
                .ifPresent(user -> {

                    int oldVersion =
                            user.getTokenVersion() == null
                                    ? 0
                                    : user.getTokenVersion();

                    user.setTokenVersion(oldVersion + 1);
                });
    }
}
