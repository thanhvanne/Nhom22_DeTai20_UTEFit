package com.utefit.service;

import com.utefit.entity.User;
import com.utefit.enums.OtpType;

public interface OtpService {

    void issueAndSend(User user, OtpType type);

    boolean consume(
            User user,
            OtpType type,
            String code
    );
}