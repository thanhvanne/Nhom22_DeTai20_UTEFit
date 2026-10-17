package com.utefit.service;

import com.utefit.dto.auth.RegisterRequest;
import com.utefit.dto.auth.ResetPasswordRequest;
import com.utefit.dto.auth.VerifyOtpRequest;
import com.utefit.entity.User;

public interface AuthService {

    String register(RegisterRequest request);

    void verifyRegistration(VerifyOtpRequest request);

    void resendRegistrationOtp(String email);

    User authenticate(
            String email,
            String rawPassword
    );

    void requestPasswordReset(String email);

    void resetPassword(ResetPasswordRequest request);

    void revokeTokensForUser(String email);
}