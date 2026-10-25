package com.utefit.service;

import com.utefit.entity.QrCheckinToken;
import com.utefit.entity.User;
import com.utefit.enums.UserRole;
import com.utefit.enums.UserStatus;
import com.utefit.repository.QrCheckinTokenRepository;
import com.utefit.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class QrCheckinService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final QrCheckinTokenRepository tokens;
    private final UserRepository users;
    private final AdminCheckinService checkins;
    private final Clock clock;

    public record IssuedToken(String value, LocalDateTime expiresAt) {}

    @Transactional
    public IssuedToken issue(String email) {
        User member = users.lockByEmailForBooking(email)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản."));
        validateMember(member);
        LocalDateTime now = LocalDateTime.now(clock);
        tokens.revokeUnused(member.getId(), now);
        byte[] entropy = new byte[32];
        RANDOM.nextBytes(entropy);
        String value = "UTEFITQR." + Base64.getUrlEncoder().withoutPadding().encodeToString(entropy);
        LocalDateTime expiry = now.plusMinutes(2);
        tokens.save(QrCheckinToken.builder().user(member).tokenHash(hash(value))
                .accountVersion(version(member)).expiresAt(expiry).build());
        return new IssuedToken(value, expiry);
    }

    @Transactional
    public void consume(String value, Long branchId) {
        if (value == null || !value.matches("UTEFITQR\\.[A-Za-z0-9_-]{43}")) {
            throw new IllegalArgumentException("Mã QR không hợp lệ hoặc đã bị sửa.");
        }
        String digest = hash(value);
        Long memberId = tokens.findUserIdByHash(digest)
                .orElseThrow(() -> new IllegalArgumentException("Mã QR không hợp lệ hoặc đã bị sửa."));
        User member = users.lockByIdForBooking(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member không tồn tại."));
        validateMember(member);
        QrCheckinToken token = tokens.lockByHash(digest).orElseThrow();
        LocalDateTime now = LocalDateTime.now(clock);
        if (token.getUsedAt() != null || !token.getExpiresAt().isAfter(now)
                || !Objects.equals(token.getAccountVersion(), version(member))) {
            throw new IllegalArgumentException("QR đã hết hạn, đã sử dụng hoặc đã được thu hồi. Hãy tạo mã mới.");
        }
        checkins.checkIn(member.getEmail(), branchId);
        token.setUsedAt(now);
    }

    private void validateMember(User member) {
        if (member.getRole() != UserRole.MEMBER || member.getStatus() != UserStatus.ACTIVE
                || !Boolean.TRUE.equals(member.getEnabled())) {
            throw new IllegalArgumentException("Tài khoản Member không hoạt động.");
        }
    }

    private int version(User member) {
        return member.getTokenVersion() == null ? 0 : member.getTokenVersion();
    }

    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Không thể tạo QR.", ex);
        }
    }
}
