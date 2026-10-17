package com.utefit.service.impl;

import com.utefit.entity.OtpCode;
import com.utefit.entity.User;
import com.utefit.enums.OtpType;
import com.utefit.repository.OtpCodeRepository;
import com.utefit.service.MailService;
import com.utefit.service.OtpService;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OtpServiceImpl implements OtpService {

    private static final SecureRandom RANDOM =
            new SecureRandom();

    private final OtpCodeRepository repository;
    private final PasswordEncoder encoder;
    private final MailService mailService;

    public OtpServiceImpl(
            OtpCodeRepository repository,
            PasswordEncoder encoder,
            MailService mailService
    ) {
        this.repository = repository;
        this.encoder = encoder;
        this.mailService = mailService;
    }

    @Override
    @Transactional
    public void issueAndSend(User user, OtpType type) {

        LocalDateTime now = LocalDateTime.now();

        // Giới hạn tối đa 5 OTP trong một giờ.
        long sent = repository
                .countByUserAndTypeAndCreatedAtAfter(
                        user,
                        type,
                        now.minusHours(1)
                );

        if (sent >= 5) {
            throw new IllegalArgumentException(
                    "Đã gửi quá nhiều OTP. Vui lòng thử lại sau."
            );
        }

        // Mỗi lần gửi OTP phải cách nhau 60 giây.
        repository
                .findTopByUserAndTypeOrderByCreatedAtDesc(
                        user,
                        type
                )
                .ifPresent(last -> {
                    if (last.getCreatedAt()
                            .isAfter(now.minusSeconds(60))) {

                        throw new IllegalArgumentException(
                                "Vui lòng chờ 60 giây trước khi gửi lại OTP."
                        );
                    }
                });

        // Vô hiệu các OTP cũ cùng loại.
        List<OtpCode> oldCodes =
                repository.findByUserAndTypeAndUsedFalse(
                        user,
                        type
                );

        for (OtpCode old : oldCodes) {
            old.setUsed(true);
            old.setUsedAt(now);
        }

        repository.saveAll(oldCodes);

        // Sinh 6 chữ số bằng SecureRandom.
        String code = String.format(
                "%06d",
                RANDOM.nextInt(1_000_000)
        );

        OtpCode otp = OtpCode.builder()
                .user(user)
                .type(type)
                .code(encoder.encode(code))
                .expiredAt(now.plusMinutes(5))
                .used(false)
                .attemptCount(0)
                .createdAt(now)
                .build();

        repository.save(otp);

        String reason = type == OtpType.REGISTER
                ? "đăng ký"
                : "đặt lại mật khẩu";

        mailService.sendOtp(
                user.getEmail(),
                code,
                reason
        );
    }

    @Override
    @Transactional
    public boolean consume(
            User user,
            OtpType type,
            String code
    ) {
        var optional = repository
                .findTopByUserAndTypeAndUsedFalseOrderByCreatedAtDesc(
                        user,
                        type
                );

        if (optional.isEmpty()) {
            return false;
        }

        OtpCode otp = optional.get();
        LocalDateTime now = LocalDateTime.now();

        // OTP hết hạn.
        if (!otp.getExpiredAt().isAfter(now)) {
            otp.setUsed(true);
            otp.setUsedAt(now);
            return false;
        }

        int attempts = otp.getAttemptCount() == null
                ? 0
                : otp.getAttemptCount();

        // Quá số lần nhập sai.
        if (attempts >= 5) {
            return false;
        }

        // OTP sai.
        if (!encoder.matches(code, otp.getCode())) {

            attempts++;
            otp.setAttemptCount(attempts);

            if (attempts >= 5) {
                otp.setUsed(true);
                otp.setUsedAt(now);
            }

            return false;
        }

        // OTP đúng: đánh dấu đã dùng.
        otp.setUsed(true);
        otp.setUsedAt(now);

        return true;
    }
}