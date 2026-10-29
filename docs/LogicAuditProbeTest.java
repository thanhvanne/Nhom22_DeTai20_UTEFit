package com.utefit.service;

import com.utefit.entity.*;
import com.utefit.enums.*;
import com.utefit.repository.*;
import com.utefit.service.impl.AdminScheduleServiceImpl;
import com.utefit.service.impl.ChatServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Audit probes: assert CURRENT unwanted behavior, not acceptance criteria for a fix.
 * Stored outside src/test so these assertions do not preserve bugs in the normal suite.
 * No database, mail, bank, socket or Cloudinary calls: all collaborators are mocks.
 */
class LogicAuditProbeTest {
    @Test void L01_cleanupCancelsMembershipWithCancellationRequestedPayment() {
        var memberships = mock(MembershipRepository.class);
        var payments = mock(PaymentRepository.class);
        var order = Membership.builder().id(1L).status(MembershipStatus.PENDING).build();
        when(memberships.findByStatusAndCreatedAtBefore(eq(MembershipStatus.PENDING), any()))
                .thenReturn(List.of(order));
        when(memberships.lockById(1L)).thenReturn(Optional.of(order));
        when(payments.existsByMembership_IdAndStatus(1L, PaymentStatus.CANCEL_REQUESTED)).thenReturn(true);
        new PendingMembershipCleanupScheduler(memberships, payments).closeAbandonedMemberships();
        assertEquals(MembershipStatus.CANCELLED, order.getStatus());
        verify(payments, never()).existsByMembership_IdAndStatus(1L, PaymentStatus.CANCEL_REQUESTED);
    }

    @Test void L03_memberLookupHasNoTrainerRelationshipBoundary() {
        var users = mock(UserRepository.class);
        var member = User.builder().id(42L).role(UserRole.MEMBER).email("private@utefit.test").build();
        when(users.findById(42L)).thenReturn(Optional.of(member));
        assertEquals("private@utefit.test", new PtPersonalScheduleViewService(users).member(42L).getEmail());
    }

    private record ChatFixture(ChatServiceImpl service, ConversationRepository conversations,
                               MessageRepository messages, UserRepository users, TrainerRequestRepository requests) {}
    private ChatFixture chat() {
        var conversations = mock(ConversationRepository.class);
        var messages = mock(MessageRepository.class);
        var users = mock(UserRepository.class);
        var requests = mock(TrainerRequestRepository.class);
        var service = new ChatServiceImpl(conversations, messages, mock(TrainerRepository.class), users,
                requests, mock(NotificationService.class), mock(SimpMessagingTemplate.class));
        return new ChatFixture(service, conversations, messages, users, requests);
    }
    private Conversation conversation(User member) {
        var pt = User.builder().id(2L).role(UserRole.PT).enabled(true).status(UserStatus.ACTIVE).email("pt@utefit.test").build();
        return Conversation.builder().id(10L).member(member)
                .trainer(Trainer.builder().id(3L).user(pt).active(true).build()).build();
    }

    @Test void L02_chatServiceAcceptsMessageFromLockedMember() {
        var f = chat();
        var member = User.builder().id(1L).role(UserRole.MEMBER).status(UserStatus.LOCKED)
                .enabled(false).email("member@utefit.test").build();
        var c = conversation(member);
        when(f.conversations.findById(10L)).thenReturn(Optional.of(c));
        when(f.users.findById(1L)).thenReturn(Optional.of(member));
        when(f.requests.existsByMember_IdAndTrainer_IdAndStatus(1L, 3L, TrainerRequestStatus.ACCEPTED)).thenReturn(true);
        when(f.messages.save(any(Message.class))).thenAnswer(i -> i.getArgument(0));
        assertDoesNotThrow(() -> f.service.sendMessage(1L, 10L, "message after lock"));
        verify(f.messages).save(any(Message.class));
    }

    @Test void L12_fetchingLimitedMessagesMarksUnreturnedMessageRead() {
        var f = chat();
        var member = User.builder().id(1L).build();
        var c = conversation(member);
        var unseen = Message.builder().id(99L).conversation(c).sender(c.getTrainer().getUser()).content("not returned").build();
        when(f.conversations.findById(10L)).thenReturn(Optional.of(c));
        when(f.messages.findTop50ByConversation_IdOrderBySentAtDesc(10L)).thenReturn(List.of());
        when(f.messages.findUnreadMessagesForUserInConversation(10L, 1L)).thenReturn(List.of(unseen));
        assertTrue(f.service.getRecentMessages(1L, 10L, 50).isEmpty());
        assertNotNull(unseen.getReadAt());
    }

    @Test void L07_groupScheduleCanBePastAndOutsideBranchOpeningHours() {
        var schedules = mock(ClassScheduleRepository.class);
        var classes = mock(FitnessClassRepository.class);
        var service = new AdminScheduleServiceImpl(schedules, classes, mock(BranchRepository.class),
                mock(ClassRegistrationRepository.class), mock(NotificationService.class),
                mock(ScheduleConflictService.class), mock(UserRepository.class));
        var branch = Branch.builder().id(5L).active(true).openingTime(LocalTime.of(6, 0))
                .closingTime(LocalTime.of(22, 0)).build();
        var fitnessClass = FitnessClass.builder().id(1L).branch(branch).status(ClassStatus.OPEN).build();
        when(classes.findDetailById(1L)).thenReturn(Optional.of(fitnessClass));
        var form = com.utefit.dto.admin.ClassScheduleFormDto.builder().fitnessClassId(1L)
                .scheduleDate(LocalDate.of(2000, 1, 1)).startTime(LocalTime.of(3, 0))
                .endTime(LocalTime.of(4, 0)).active(true).build();
        assertDoesNotThrow(() -> service.createSchedule(form));
        verify(schedules).save(any(ClassSchedule.class));
    }
}
