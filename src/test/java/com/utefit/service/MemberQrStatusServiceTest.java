package com.utefit.service;

import com.utefit.entity.*;
import com.utefit.enums.*;
import com.utefit.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import java.time.*;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MemberQrStatusServiceTest {
    private final UserRepository users = mock(UserRepository.class);
    private final CheckinRepository history = mock(CheckinRepository.class);
    private final QrCheckinTokenRepository tokens = mock(QrCheckinTokenRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-26T03:00:00Z"), ZoneOffset.UTC);
    private final MemberQrStatusService service = new MemberQrStatusService(users, history, tokens, clock);
    private User member;
    private QrCheckinToken token;

    @BeforeEach
    void setup() {
        member = User.builder().id(10L).email("member@utefit.test").role(UserRole.MEMBER)
                .status(UserStatus.ACTIVE).enabled(true).tokenVersion(0).build();
        token = QrCheckinToken.builder().id(30L).user(member).accountVersion(0)
                .expiresAt(LocalDateTime.now(clock).plusSeconds(120)).build();
        when(users.findByEmail(member.getEmail())).thenReturn(Optional.of(member));
        when(tokens.findByIdAndUser_Id(30L, 10L)).thenReturn(Optional.of(token));
        when(history.findFirstByUser_IdAndCheckOutAtIsNullOrderByCheckInAtDescIdDesc(10L)).thenReturn(Optional.empty());
        when(history.findFirstByUser_IdOrderByCheckInAtDescIdDesc(10L)).thenReturn(Optional.empty());
    }

    @Test
    void statusIsReadOnlyAndUsesServerExpiry() {
        var state = service.status(member.getEmail(), 30L);
        assertEquals("NONE", state.attendanceState());
        assertEquals("VALID", state.qrState());
        assertEquals(120_000L, state.expiresAt() - state.serverTime());
        assertTrue(state.canIssue());
        assertNull(token.getUsedAt());
        verify(tokens, never()).save(any());
        verify(history, never()).save(any());
    }

    @Test
    void consumedQrAndOpenSessionAreReportedTogether() {
        token.setUsedAt(LocalDateTime.now(clock));
        Checkin open = Checkin.builder().id(40L).user(member).branch(Branch.builder().name("Test branch").build())
                .checkInAt(LocalDateTime.now(clock)).build();
        when(history.findFirstByUser_IdAndCheckOutAtIsNullOrderByCheckInAtDescIdDesc(10L)).thenReturn(Optional.of(open));
        var state = service.status(member.getEmail(), 30L);
        assertEquals("OPEN", state.attendanceState());
        assertEquals("UNAVAILABLE", state.qrState());
        assertEquals("Test branch", state.branchName());
        assertFalse(state.canIssue());
    }

    @Test
    void checkoutDoesNotMakeConsumedQrValidAgain() {
        token.setUsedAt(LocalDateTime.now(clock).minusSeconds(10));
        Checkin closed = Checkin.builder().id(40L).user(member).branch(Branch.builder().name("Test branch").build())
                .checkInAt(LocalDateTime.now(clock).minusSeconds(10)).checkOutAt(LocalDateTime.now(clock)).build();
        when(history.findFirstByUser_IdOrderByCheckInAtDescIdDesc(10L)).thenReturn(Optional.of(closed));
        var state = service.status(member.getEmail(), 30L);
        assertEquals("CHECKED_OUT", state.attendanceState());
        assertEquals("UNAVAILABLE", state.qrState());
        assertTrue(state.canIssue());
        assertNotNull(state.checkOutAt());
    }

    @Test
    void exactExpiryBoundaryIsExpiredWithoutMutatingToken() {
        token.setExpiresAt(LocalDateTime.now(clock));
        assertEquals("EXPIRED", service.status(member.getEmail(), 30L).qrState());
        assertNull(token.getUsedAt());
    }

    @Test
    void revokedOrAccountVersionChangedTokenIsUnavailableNotProofOfCheckin() {
        token.setUsedAt(LocalDateTime.now(clock));
        assertEquals("NONE", service.status(member.getEmail(), 30L).attendanceState());
        assertEquals("UNAVAILABLE", service.status(member.getEmail(), 30L).qrState());
        token.setUsedAt(null);
        member.setTokenVersion(1);
        assertEquals("UNAVAILABLE", service.status(member.getEmail(), 30L).qrState());
    }

    @Test
    void tokenFromAnotherMemberOrUnknownIdIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.status(member.getEmail(), 99L));
        verify(tokens).findByIdAndUser_Id(99L, member.getId());
    }

    @Test
    void disabledLockedAndNonMemberAccountsAreRejected() {
        member.setEnabled(false);
        assertThrows(AccessDeniedException.class, () -> service.status(member.getEmail(), null));
        member.setEnabled(true);
        member.setStatus(UserStatus.LOCKED);
        assertThrows(AccessDeniedException.class, () -> service.status(member.getEmail(), null));
        member.setStatus(UserStatus.ACTIVE);
        member.setRole(UserRole.PT);
        assertThrows(AccessDeniedException.class, () -> service.status(member.getEmail(), null));
    }

    @Test
    void issuingQrWithOpenSessionIsBlockedBeforeRevokingOrCreatingTokens() {
        when(users.lockByEmailForBooking(member.getEmail())).thenReturn(Optional.of(member));
        when(history.existsByUser_IdAndCheckOutAtIsNull(member.getId())).thenReturn(true);
        var issuance = new QrCheckinService(tokens, users, mock(AdminCheckinService.class), clock, history);
        assertThrows(IllegalArgumentException.class, () -> issuance.issue(member.getEmail()));
        verify(tokens, never()).revokeUnused(anyLong(), any());
        verify(tokens, never()).save(any());
    }
}
