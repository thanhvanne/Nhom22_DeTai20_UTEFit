package com.utefit.service.impl;

import com.utefit.service.MailService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailServiceImpl implements MailService {

    private final JavaMailSender sender;
    private final String from;

    public MailServiceImpl(
            JavaMailSender sender,
            @Value("${utefit.mail.from}") String from
    ) {
        this.sender = sender;
        this.from = from;
    }

    @Override
    public void sendOtp(
            String recipient,
            String code,
            String reason
    ) {
        SimpleMailMessage mail = new SimpleMailMessage();

        mail.setFrom(from);
        mail.setTo(recipient);
        mail.setSubject("UTEFit - Mã OTP " + reason);

        mail.setText(
                "Mã xác thực UTEFit của bạn: " + code
                        + "\n\nMã có hiệu lực trong 5 phút."
                        + "\nKhông chia sẻ mã này cho người khác."
        );

        sender.send(mail);
    }
}