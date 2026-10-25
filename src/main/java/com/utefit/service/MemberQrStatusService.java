package com.utefit.service;

import com.utefit.entity.Checkin;
import com.utefit.entity.QrCheckinToken;
import com.utefit.entity.User;
import com.utefit.enums.UserRole;
import com.utefit.enums.UserStatus;
import com.utefit.enums.QrCheckinPurpose;
import com.utefit.repository.CheckinRepository;
import com.utefit.repository.QrCheckinTokenRepository;
import com.utefit.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class MemberQrStatusService {
    private final UserRepository users;
    private final CheckinRepository history;
    private final QrCheckinTokenRepository tokens;
    private final Clock clock;

    public record Status(String attendanceState, String qrState, Long expiresAt, long serverTime,
                         Long checkinId, String branchName, LocalDateTime checkInAt,
                         LocalDateTime checkOutAt, boolean canIssue, String qrPurpose, boolean canCheckout) {}

    @Transactional(readOnly = true)
    public Status status(String email, Long tokenId) {
        User member = users.findByEmail(email).orElseThrow(() -> new AccessDeniedException("Tài khoản không hợp lệ."));
        if (member.getRole() != UserRole.MEMBER || member.getStatus() != UserStatus.ACTIVE
                || !Boolean.TRUE.equals(member.getEnabled())) {
            throw new AccessDeniedException("Chỉ Member đang hoạt động được xem trạng thái Check-in.");
        }
        QrCheckinToken token = null;
        if (tokenId != null) {
            token = tokens.findByIdAndUser_Id(tokenId, member.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy QR của bạn."));
        }
        Checkin current = history.findFirstByUser_IdAndCheckOutAtIsNullOrderByCheckInAtDescIdDesc(member.getId())
                .orElseGet(() -> history.findFirstByUser_IdOrderByCheckInAtDescIdDesc(member.getId()).orElse(null));
        boolean open = current != null && current.getCheckOutAt() == null;
        String attendance = current == null ? "NONE" : open ? "OPEN" : "CHECKED_OUT";
        String qrState = "NONE";
        Long expiresAt = null;
        String qrPurpose = token == null ? "NONE" : token.getPurpose().name();
        var now = clock.instant();
        if (token != null) {
            expiresAt = token.getExpiresAt().atZone(clock.getZone()).toInstant().toEpochMilli();
            int version = member.getTokenVersion() == null ? 0 : member.getTokenVersion();
            boolean wrongSession = token.getPurpose() == QrCheckinPurpose.CHECK_OUT
                    && (!open || token.getCheckin() == null || !token.getCheckin().getId().equals(current.getId()));
            if (token.getUsedAt() != null || !Objects.equals(token.getAccountVersion(), version) || wrongSession) {
                qrState = "UNAVAILABLE";
            } else {
                qrState = now.toEpochMilli() >= expiresAt ? "EXPIRED" : "VALID";
            }
        }
        return new Status(attendance, qrState, expiresAt, now.toEpochMilli(),
                current == null ? null : current.getId(), current == null ? null : current.getBranch().getName(),
                current == null ? null : current.getCheckInAt(), current == null ? null : current.getCheckOutAt(), !open, qrPurpose, open);
    }
}
