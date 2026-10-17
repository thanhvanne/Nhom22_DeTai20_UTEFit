package com.utefit.service;

public interface MailService {

    void sendOtp(
            String recipient,
            String code,
            String reason
    );
}