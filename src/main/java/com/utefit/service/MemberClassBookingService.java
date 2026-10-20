
package com.utefit.service;

import com.utefit.entity.ClassRegistration;
import com.utefit.entity.ClassSchedule;
import com.utefit.entity.FitnessClass;
import com.utefit.entity.Membership;
import com.utefit.entity.User;
import com.utefit.enums.ClassStatus;
import com.utefit.enums.MembershipStatus;
import com.utefit.enums.RegistrationStatus;
import com.utefit.enums.UserRole;
import com.utefit.enums.UserStatus;
import com.utefit.repository.ClassRegistrationRepository;
import com.utefit.repository.MemberClassScheduleRepository;
import com.utefit.repository.MembershipRepository;
import com.utefit.repository.UserRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class MemberClassBookingService {

    private static final ZoneId GYM_ZONE =
            ZoneId.of("Asia/Ho_Chi_Minh");

    private static final int PAGE_SIZE = 8;

    private static final Set<ClassStatus> SHOWN_STATUSES =
            Set.of(ClassStatus.OPEN, ClassStatus.FULL);

    private final UserRepository users;
    private final MembershipRepository memberships;
    private final MemberClassScheduleRepository schedules;
    private final ClassRegistrationRepository registrations;

    public MemberClassBookingService(
            UserRepository users,
            MembershipRepository memberships,
            MemberClassScheduleRepository schedules,
            ClassRegistrationRepository registrations
    ) {
        this.users = users;
        this.memberships = memberships;
        this.schedules = schedules;
        this.registrations = registrations;
    }

    // ==============================
    // 1. DANH SÁCH LỊCH LỚP CHO MEMBER
    // ==============================

    @Transactional(readOnly = true)
    public Page<ScheduleView> availableSchedules(
            String email,
            String keyword,
            Long branchId,
            int page
    ) {
        User member = requireMember(email);
        LocalDateTime now = now();

        List<Membership> myMemberships = memberships
                .findByUser_IdOrderByCreatedAtDesc(
                        member.getId()
                );

        Page<ClassSchedule> result =
                schedules.searchMemberSchedules(
                        now.toLocalDate(),
                        SHOWN_STATUSES,
                        branchId,
                        normalize(keyword),
                        PageRequest.of(
                                Math.max(
                                        0,
                                        Math.min(page, 10000)
                                ),
                                PAGE_SIZE
                        )
                );

        return result.map(schedule -> {

            FitnessClass fc = schedule.getFitnessClass();

            long booked = registrations
                    .countBySchedule_IdAndStatus(
                            schedule.getId(),
                            RegistrationStatus.REGISTERED
                    );

            boolean registered = registrations
                    .findByUser_IdAndSchedule_Id(
                            member.getId(),
                            schedule.getId()
                    )
                    .map(r ->
                            r.getStatus()
                                    == RegistrationStatus.REGISTERED
                    )
                    .orElse(false);

            boolean future =
                    startOf(schedule).isAfter(now);

            boolean membershipOK = hasMembership(
                    myMemberships,
                    fc.getBranch().getId(),
                    now.toLocalDate(),
                    schedule.getScheduleDate()
            );

            boolean available =
                    fc.getStatus() == ClassStatus.OPEN
                            && fc.getCapacity() != null
                            && fc.getCapacity() > 0
                            && booked < fc.getCapacity();

            boolean canBook =
                    !registered
                            && future
                            && available
                            && membershipOK;

            boolean canCancel =
                    registered && future;

            String reason =
                    registered
                            ? "Đã đăng ký"
                            : !future
                            ? "Lịch đã bắt đầu"
                            : !membershipOK
                            ? "Cần gói ACTIVE còn hạn tại chi nhánh này"
                            : fc.getStatus() != ClassStatus.OPEN
                            ? "Lớp không mở đăng ký"
                            : booked >= fc.getCapacity()
                            ? "Lịch đã đủ chỗ"
                            : "Còn chỗ";

            String trainerName = "Đang cập nhật";

            if (fc.getTrainer() != null
                    && fc.getTrainer().getUser() != null) {
                trainerName =
                        fc.getTrainer()
                                .getUser()
                                .getFullName();
            }

            return new ScheduleView(
                    schedule.getId(),
                    fc.getName(),
                    fc.getCategory().getName(),
                    fc.getBranch().getName(),
                    trainerName,
                    schedule.getRoom(),
                    schedule.getScheduleDate(),
                    schedule.getStartTime(),
                    schedule.getEndTime(),
                    fc.getCapacity(),
                    booked,
                    registered,
                    canBook,
                    canCancel,
                    reason
            );
        });
    }

    // ==============================
    // 2. ĐĂNG KÝ LỚP
    // ==============================

    @Transactional
    public void book(String email, Long scheduleId) {

        // Khóa Member trước, sau đó khóa Schedule.
        // Các request đặt/hủy tuân thủ cùng thứ tự này.
        User member = lockedMember(email);

        ClassSchedule schedule =
                lockedSchedule(scheduleId);

        LocalDateTime now = now();

        // Lịch phải còn hoạt động, lớp OPEN,
        // còn trong tương lai.
        validateOpenSchedule(schedule, now);

        FitnessClass fc = schedule.getFitnessClass();

        // Phải có Membership ACTIVE.
        // Membership phải thuộc chi nhánh của lớp,
        // hợp lệ hôm nay và kéo dài tới ngày học.
        if (!hasMembership(
                memberships
                        .findByUser_IdOrderByCreatedAtDesc(
                                member.getId()
                        ),
                fc.getBranch().getId(),
                now.toLocalDate(),
                schedule.getScheduleDate()
        )) {
            throw new IllegalArgumentException(
                    "Cần Membership ACTIVE còn hạn "
                            + "tại đúng chi nhánh và ngày học."
            );
        }

        // Kiểm tra đăng ký trùng.
        ClassRegistration old = registrations
                .findByUser_IdAndSchedule_Id(
                        member.getId(),
                        scheduleId
                )
                .orElse(null);

        if (old != null
                && old.getStatus()
                != RegistrationStatus.CANCELLED) {
            throw new IllegalArgumentException(
                    "Bạn đã đăng ký lịch này trước đó."
            );
        }

        // Kiểm tra capacity sau khi đã khóa Schedule.
        long booked = registrations
                .countBySchedule_IdAndStatus(
                        scheduleId,
                        RegistrationStatus.REGISTERED
                );

        if (booked >= fc.getCapacity()) {
            throw new IllegalArgumentException(
                    "Lịch lớp đã đủ chỗ."
            );
        }

        // Chống trùng giờ giữa các lớp khác nhau
        // mà chính Member này đang đăng ký.
        List<ClassRegistration> sameDay =
                registrations.findOtherRegistrationsOnDate(
                        member.getId(),
                        RegistrationStatus.REGISTERED,
                        schedule.getScheduleDate(),
                        scheduleId
                );

        for (ClassRegistration other : sameDay) {

            ClassSchedule another = other.getSchedule();

            // Hai khoảng [start,end) bị trùng khi
            // startA < endB và startB < endA.
            boolean overlap =
                    schedule.getStartTime()
                            .isBefore(another.getEndTime())
                            && another.getStartTime()
                            .isBefore(schedule.getEndTime());

            if (overlap) {
                throw new IllegalArgumentException(
                        "Lịch này bị trùng giờ với lớp "
                                + "bạn đã đăng ký: "
                                + another.getFitnessClass().getName()
                );
            }
        }

        if (old == null) {

            // Lần đầu đăng ký lịch này.
            old = ClassRegistration.builder()
                    .user(member)
                    .schedule(schedule)
                    .status(RegistrationStatus.REGISTERED)
                    .registeredAt(now)
                    .build();

        } else {

            // Đã hủy trước đó thì kích hoạt lại
            // bản ghi cũ, tránh vi phạm UNIQUE.
            old.setStatus(RegistrationStatus.REGISTERED);
            old.setRegisteredAt(now);
        }

        registrations.saveAndFlush(old);
    }

    // ==============================
    // 3. HỦY ĐĂNG KÝ
    // ==============================

    @Transactional
    public void cancel(String email, Long scheduleId) {

        User member = lockedMember(email);

        ClassSchedule schedule =
                lockedSchedule(scheduleId);

        // Chỉ tìm bản ghi thuộc chính Member.
        ClassRegistration registration =
                registrations
                        .findByUser_IdAndSchedule_Id(
                                member.getId(),
                                scheduleId
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Bạn chưa đăng ký lịch này."
                                )
                        );

        if (registration.getStatus()
                != RegistrationStatus.REGISTERED) {
            throw new IllegalArgumentException(
                    "Lịch này không ở trạng thái REGISTERED."
            );
        }

        if (!startOf(schedule).isAfter(now())) {
            throw new IllegalArgumentException(
                    "Lịch đã bắt đầu, không thể hủy."
            );
        }

        // Giữ lịch sử, không xóa bản ghi.
        registration.setStatus(
                RegistrationStatus.CANCELLED
        );

        registrations.saveAndFlush(registration);
    }

    // ==============================
    // 4. LỊCH CÁ NHÂN
    // ==============================

    @Transactional(readOnly = true)
    public List<BookingView> mySchedule(String email) {

        User member = requireMember(email);
        LocalDateTime now = now();

        return registrations
                .findHistoryByUserId(member.getId())
                .stream()
                .map(r -> {

                    ClassSchedule s = r.getSchedule();
                    FitnessClass fc = s.getFitnessClass();

                    boolean canCancel =
                            r.getStatus()
                                    == RegistrationStatus.REGISTERED
                                    && startOf(s).isAfter(now);

                    return new BookingView(
                            s.getId(),
                            fc.getName(),
                            fc.getBranch().getName(),
                            s.getRoom(),
                            s.getScheduleDate(),
                            s.getStartTime(),
                            s.getEndTime(),
                            r.getStatus().name(),
                            canCancel
                    );
                })
                .toList();
    }

    // ==============================
    // 5. VALIDATION
    // ==============================

    private void validateOpenSchedule(
            ClassSchedule schedule,
            LocalDateTime now
    ) {
        FitnessClass fc = schedule.getFitnessClass();

        if (!Boolean.TRUE.equals(
                schedule.getActive()
        )
                || !Boolean.TRUE.equals(
                fc.getBranch().getActive()
        )
                || !Boolean.TRUE.equals(
                fc.getCategory().getActive()
        )
                || fc.getStatus() != ClassStatus.OPEN) {

            throw new IllegalArgumentException(
                    "Lịch/lớp không còn mở đăng ký."
            );
        }

        if (fc.getCapacity() == null
                || fc.getCapacity() <= 0) {

            throw new IllegalArgumentException(
                    "Sức chứa lớp không hợp lệ."
            );
        }

        if (schedule.getStartTime() == null
                || schedule.getEndTime() == null
                || !schedule.getStartTime().isBefore(
                schedule.getEndTime()
        )) {

            throw new IllegalArgumentException(
                    "Giờ học không hợp lệ."
            );
        }

        if (!startOf(schedule).isAfter(now)) {
            throw new IllegalArgumentException(
                    "Không thể đặt lịch đã bắt đầu."
            );
        }
    }

    private boolean hasMembership(
            List<Membership> all,
            Long branchId,
            LocalDate today,
            LocalDate classDate
    ) {
        return all.stream().anyMatch(m ->
                m.getStatus() == MembershipStatus.ACTIVE

                        && m.getBranch().getId().equals(branchId)

                        && m.getStartDate() != null
                        && m.getEndDate() != null

                        && !m.getStartDate().isAfter(today)
                        && !m.getEndDate().isBefore(today)

                        // Gói phải còn hạn tới ngày diễn ra lớp.
                        && !m.getEndDate().isBefore(classDate)
        );
    }

    // ==============================
    // 6. AUTHENTICATION / OWNERSHIP
    // ==============================

    private User requireMember(String email) {

        User member = users.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Không tìm thấy tài khoản."
                        )
                );

        validateMember(member);
        return member;
    }

    private User lockedMember(String email) {

        User member = users
                .lockByEmailForBooking(email)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Không tìm thấy tài khoản."
                        )
                );

        validateMember(member);
        return member;
    }

    private void validateMember(User member) {

        if (member.getRole() != UserRole.MEMBER
                || member.getStatus() != UserStatus.ACTIVE
                || !Boolean.TRUE.equals(
                member.getEnabled()
        )) {

            throw new IllegalArgumentException(
                    "Tài khoản MEMBER không hợp lệ."
            );
        }
    }

    private ClassSchedule lockedSchedule(Long scheduleId) {

        if (scheduleId == null || scheduleId <= 0) {
            throw new IllegalArgumentException(
                    "Mã lịch lớp không hợp lệ."
            );
        }

        return schedules.lockById(scheduleId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Không tìm thấy lịch lớp."
                        )
                );
    }

    // ==============================
    // 7. HELPERS
    // ==============================

    private LocalDateTime now() {
        return LocalDateTime.now(GYM_ZONE);
    }

    private LocalDateTime startOf(ClassSchedule s) {
        return LocalDateTime.of(
                s.getScheduleDate(),
                s.getStartTime()
        );
    }

    private String normalize(String keyword) {

        if (keyword == null || keyword.isBlank()) {
            return null;
        }

        String value = keyword.trim()
                .toLowerCase(Locale.ROOT);

        if (value.length() > 100) {
            throw new IllegalArgumentException(
                    "Từ khóa tìm kiếm quá dài."
            );
        }

        return "%" + value + "%";
    }

    // ==============================
    // DTO HIỂN THỊ CHO THYMELEAF
    // ==============================

    public record ScheduleView(
            Long id,
            String className,
            String categoryName,
            String branchName,
            String trainerName,
            String room,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime,
            Integer capacity,
            long booked,
            boolean registered,
            boolean canBook,
            boolean canCancel,
            String reason
    ) {}

    public record BookingView(
            Long scheduleId,
            String className,
            String branchName,
            String room,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime,
            String status,
            boolean canCancel
    ) {}
}
