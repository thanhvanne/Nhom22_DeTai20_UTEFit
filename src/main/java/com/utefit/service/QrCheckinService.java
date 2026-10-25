package com.utefit.service;

import com.utefit.entity.QrCheckinToken;
import com.utefit.entity.User;
import com.utefit.entity.Checkin;
import com.utefit.enums.QrCheckinPurpose;
import com.utefit.enums.UserRole;
import com.utefit.enums.UserStatus;
import com.utefit.repository.QrCheckinTokenRepository;
import com.utefit.repository.CheckinRepository;
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
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import org.springframework.security.access.AccessDeniedException;

@Service
@RequiredArgsConstructor
public class QrCheckinService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final QrCheckinTokenRepository tokens;
    private final UserRepository users;
    private final AdminCheckinService checkins;
    private final Clock clock;
    private final CheckinRepository history;

    public record IssuedToken(String value, LocalDateTime expiresAt, Long id) {}
    public record ScanResult(QrCheckinPurpose purpose, Long checkinId, String memberName, String branchName, LocalDateTime performedAt) {
        public String message() {
            return (purpose == QrCheckinPurpose.CHECK_OUT ? "Check-out thành công" : "Check-in thành công")
                    + " — " + memberName + " · " + branchName + " · "
                    + performedAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
        }
    }

    @Transactional
    public IssuedToken issue(String email) {
        return issueFor(email, QrCheckinPurpose.CHECK_IN);
    }

    @Transactional
    public IssuedToken issueCheckout(String email) {
        return issueFor(email, QrCheckinPurpose.CHECK_OUT);
    }

    private IssuedToken issueFor(String email, QrCheckinPurpose purpose) {
        User member = users.lockByEmailForBooking(email)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản."));
        validateMember(member);
        Checkin target = null;
        if (purpose == QrCheckinPurpose.CHECK_IN && history.existsByUser_IdAndCheckOutAtIsNull(member.getId())) {
            throw new IllegalArgumentException("Bạn đang tập và chưa checkout. Không thể tạo QR Check-in mới.");
        }
        if (purpose == QrCheckinPurpose.CHECK_OUT) {
            target = history.findFirstByUser_IdAndCheckOutAtIsNullOrderByCheckInAtDescIdDesc(member.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Bạn không có lượt đang tập để tạo QR Check-out."));
        }
        LocalDateTime now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MILLIS);
        tokens.revokeUnused(member.getId(), now);
        byte[] entropy = new byte[32];
        RANDOM.nextBytes(entropy);
        String prefix = purpose == QrCheckinPurpose.CHECK_OUT ? "UTEFITQO." : "UTEFITQR.";
        String value = prefix + Base64.getUrlEncoder().withoutPadding().encodeToString(entropy);
        LocalDateTime expiry = now.plusMinutes(2);
        QrCheckinToken saved = tokens.save(QrCheckinToken.builder().user(member).tokenHash(hash(value))
                .accountVersion(version(member)).expiresAt(expiry).purpose(purpose).checkin(target).build());
        return new IssuedToken(value, expiry, saved.getId());
    }

    @Transactional
    public ScanResult consumeAsAdmin(String scannerEmail, String value, Long branchId) {
        User scanner = users.findByEmail(scannerEmail).orElseThrow(() -> new AccessDeniedException("Tài khoản quét không hợp lệ."));
        if (scanner.getRole() != UserRole.ADMIN || scanner.getStatus() != UserStatus.ACTIVE || !Boolean.TRUE.equals(scanner.getEnabled())) {
            throw new AccessDeniedException("Chỉ Admin đang hoạt động được quét QR tại đây.");
        }
        return consume(value, branchId);
    }

    @Transactional
    public ScanResult consume(String value, Long branchId) {
        if (value == null || !value.matches("UTEFITQ[RO]\\.[A-Za-z0-9_-]{43}")) {
            throw new IllegalArgumentException("Mã QR không hợp lệ hoặc đã bị sửa.");
        }
        String digest = hash(value);
        Long memberId = tokens.findUserIdByHash(digest)
                .orElseThrow(() -> new IllegalArgumentException("Mã QR không hợp lệ hoặc đã bị sửa."));
        User member = users.lockByIdForBooking(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member không tồn tại."));
        validateMember(member);
        QrCheckinToken token = tokens.lockByHash(digest).orElseThrow();
        if ((token.getPurpose() == QrCheckinPurpose.CHECK_IN && !value.startsWith("UTEFITQR."))
                || (token.getPurpose() == QrCheckinPurpose.CHECK_OUT && !value.startsWith("UTEFITQO."))) {
            throw new IllegalArgumentException("Mục đích QR không khớp mã đã cấp. Hãy tạo mã mới.");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        if (token.getUsedAt() != null || !token.getExpiresAt().isAfter(now)
                || !Objects.equals(token.getAccountVersion(), version(member))) {
            throw new IllegalArgumentException("QR đã hết hạn, đã sử dụng hoặc đã được thu hồi. Hãy tạo mã mới.");
        }
        Checkin current;
        if (token.getPurpose() == QrCheckinPurpose.CHECK_IN && token.getCheckin() == null) {
            checkins.checkIn(member.getEmail(), branchId);
            current = history.findFirstByUser_IdAndCheckOutAtIsNullOrderByCheckInAtDescIdDesc(memberId).orElseThrow();
        } else if (token.getPurpose() == QrCheckinPurpose.CHECK_OUT && token.getCheckin() != null) {
            current = history.findFirstByUser_IdAndCheckOutAtIsNullOrderByCheckInAtDescIdDesc(memberId)
                    .orElseThrow(() -> new IllegalArgumentException("Không có lượt đang tập để Check-out. QR không thể dùng lại."));
            if (!current.getId().equals(token.getCheckin().getId()) || !current.getUser().getId().equals(memberId)
                    || !current.getBranch().getId().equals(branchId)) {
                throw new IllegalArgumentException("QR Check-out không thuộc lượt tập hoặc chi nhánh đang được xác nhận.");
            }
            checkins.checkOut(current.getId());
        } else {
            throw new IllegalArgumentException("Mục đích QR không hợp lệ. Hãy tạo mã mới.");
        }
        token.setUsedAt(now);
        return new ScanResult(token.getPurpose(), current.getId(), member.getFullName(), current.getBranch().getName(),
                token.getPurpose() == QrCheckinPurpose.CHECK_OUT ? current.getCheckOutAt() : current.getCheckInAt());
    }

    private void validateMember(User member) {
        if (member.getRole() != UserRole.MEMBER || member.getStatus() != UserStatus.ACTIVE
                || !Boolean.TRUE.equals(member.getEnabled())) {
            throw new IllegalArgumentException("Tài khoản Member không hoạt động. Nếu đang tập, nhờ Admin/Manager checkout thủ công sau khi xác minh tại quầy.");
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
