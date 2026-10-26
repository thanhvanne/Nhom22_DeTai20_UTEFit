package com.utefit.service;

import com.utefit.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.util.TreeSet;

@Component
@lombok.extern.slf4j.Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "utefit.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class MembershipBookingPolicyScheduler {
    private final ClassRegistrationRepository registrations;
    private final PersonalTrainingScheduleRepository personal;
    private final MembershipBookingPolicyService policy;

    @Scheduled(fixedDelay = 60000, initialDelay = 60000)
    public void revalidateFutureBookings() {
        LocalDate today = LocalDate.now(SepayPaymentService.ZONE);
        var members = new TreeSet<Long>();
        for (int page = 0; ; page++) {
            var group = registrations.findFutureMemberIds(today, PageRequest.of(page, 100));
            var sessions = personal.findFutureMemberIds(today, PageRequest.of(page, 100));
            members.addAll(group);
            members.addAll(sessions);
            if (group.size() < 100 && sessions.size() < 100) break;
        }
        for (Long memberId : members) {
            try { policy.revalidateMember(memberId); }
            catch (RuntimeException ex) { log.warn("Membership booking revalidation failed for user ID {}; retry next cycle ({})", memberId, ex.getClass().getSimpleName()); }
        }
    }
}
