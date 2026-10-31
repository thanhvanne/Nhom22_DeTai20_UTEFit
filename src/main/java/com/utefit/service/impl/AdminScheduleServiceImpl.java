
package com.utefit.service.impl;

import com.utefit.dto.admin.ClassScheduleFormDto;
import com.utefit.entity.Branch;
import com.utefit.entity.ClassSchedule;
import com.utefit.entity.FitnessClass;
import com.utefit.entity.Trainer;
import com.utefit.enums.ClassStatus;
import com.utefit.enums.NotificationType;
import com.utefit.enums.RegistrationStatus;
import com.utefit.repository.BranchRepository;
import com.utefit.repository.ClassRegistrationRepository;
import com.utefit.repository.ClassScheduleRepository;
import com.utefit.repository.FitnessClassRepository;
import com.utefit.service.AdminScheduleService;
import com.utefit.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminScheduleServiceImpl
        implements AdminScheduleService {

    private final ClassScheduleRepository scheduleRepository;
    private final FitnessClassRepository fitnessClassRepository;
    private final BranchRepository branchRepository;

    // GD5-C: Tim Member da dang ky lich hoc.
    private final ClassRegistrationRepository classRegistrationRepository;

    // GD5-C: Goi Service cua thanh vien B.
    private final NotificationService notificationService;
    private final com.utefit.service.ScheduleConflictService conflicts;
    private final com.utefit.repository.UserRepository userRepository;

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    @Transactional(readOnly = true)
    public Page<ClassSchedule> getSchedules(
            Long fitnessClassId,
            Long branchId,
            LocalDate scheduleDate,
            Boolean active,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(
                Math.max(0, page),
                Math.max(1, Math.min(100, size)),
                Sort.by(Sort.Direction.ASC, "scheduleDate")
                        .and(Sort.by(
                                Sort.Direction.ASC, "startTime"))
        );

        return scheduleRepository.searchAdminSchedules(
                fitnessClassId,
                branchId,
                scheduleDate,
                active,
                pageable
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ClassSchedule getScheduleById(Long id) {
        return scheduleRepository.findDetailById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Không tìm thấy lịch lớp tập với ID: " + id
                        ));
    }

    @Override
    @Transactional(readOnly = true)
    public ClassScheduleFormDto getScheduleFormById(Long id) {
        ClassSchedule s = getScheduleById(id);

        return ClassScheduleFormDto.builder()
                .id(s.getId())
                .fitnessClassId(s.getFitnessClass().getId())
                .scheduleDate(s.getScheduleDate())
                .startTime(s.getStartTime())
                .endTime(s.getEndTime())
                .room(s.getRoom())
                .active(s.getActive())
                .build();
    }

    @Override
    @Transactional
    public void createSchedule(ClassScheduleFormDto dto) {
        com.utefit.validation.FormChecks.validate(dto);
        FitnessClass fitnessClass =
                validateScheduleBusinessRules(dto, null);

        ClassSchedule schedule = ClassSchedule.builder()
                .fitnessClass(fitnessClass)
                .scheduleDate(dto.getScheduleDate())
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .room(dto.getRoom() != null
                        && !dto.getRoom().trim().isEmpty()
                        ? dto.getRoom().trim() : null)
                .active(dto.getActive() != null
                        ? dto.getActive() : true)
                .build();

        scheduleRepository.save(schedule);
    }

    @Override
    @Transactional
    public void updateSchedule(Long id, ClassScheduleFormDto dto) {
        com.utefit.validation.FormChecks.validate(dto);
        ClassSchedule schedule = getScheduleById(id);
        if (!schedule.getScheduleDate().atTime(schedule.getStartTime()).isAfter(java.time.LocalDateTime.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh"))))
            throw new IllegalStateException("Không thay đổi buổi học đã bắt đầu; cần giữ lịch sử.");

        // GD5-C: Kiem tra Admin co tat lich hay khong.
        boolean cancelledByEdit =
                Boolean.TRUE.equals(schedule.getActive())
                        && Boolean.FALSE.equals(dto.getActive());

        // Luu thong tin lich CU truoc khi thay doi.
        String cancelledContent = cancelledByEdit
                ? cancellationContent(schedule)
                : null;

        List<Long> membersToNotify = cancelledByEdit
                ? getRegisteredMemberIds(id)
                : List.of();

        long occupied = classRegistrationRepository.countBySchedule_IdAndStatus(
                id, RegistrationStatus.REGISTERED);
        if (occupied > 0 && !cancelledByEdit && (
                !schedule.getFitnessClass().getId().equals(dto.getFitnessClassId())
                        || !schedule.getScheduleDate().equals(dto.getScheduleDate())
                        || !schedule.getStartTime().equals(dto.getStartTime())
                        || !schedule.getEndTime().equals(dto.getEndTime())
                        || !java.util.Objects.equals(schedule.getRoom(),
                        dto.getRoom() == null || dto.getRoom().isBlank() ? null : dto.getRoom().trim()))) {
            throw new IllegalStateException("Lịch đã có người đăng ký; hủy lịch trước khi đổi thời gian/phòng/lớp.");
        }

        // Giu nguyen business rules Giai doan 4.
        FitnessClass fitnessClass =
                validateScheduleBusinessRules(dto, id);

        schedule.setFitnessClass(fitnessClass);
        schedule.setScheduleDate(dto.getScheduleDate());
        schedule.setStartTime(dto.getStartTime());
        schedule.setEndTime(dto.getEndTime());

        schedule.setRoom(
                dto.getRoom() != null
                        && !dto.getRoom().trim().isEmpty()
                        ? dto.getRoom().trim()
                        : null
        );

        schedule.setActive(
                dto.getActive() != null
                        ? dto.getActive()
                        : true
        );

        scheduleRepository.save(schedule);

        // GD5-C: Neu lich chuyen ACTIVE -> INACTIVE.
        if (cancelledByEdit) {
            classRegistrationRepository.cancelRegistrationsForSchedule(
                    id, RegistrationStatus.REGISTERED, RegistrationStatus.CANCELLED);
            notifyScheduleCancellation(
                    id,
                    membersToNotify,
                    cancelledContent
            );
        }
    }

    @Override
    @Transactional
    public void toggleActive(Long id) {
        ClassSchedule schedule = getScheduleById(id);
        if (!schedule.getScheduleDate().atTime(schedule.getStartTime()).isAfter(java.time.LocalDateTime.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh"))))
            throw new IllegalStateException("Không thay đổi buổi học đã bắt đầu; cần giữ lịch sử.");

        boolean nextActive =
                !Boolean.TRUE.equals(schedule.getActive());

        // GD5-C: Neu dang dong ca thi lay Member.
        String cancelledContent = !nextActive
                ? cancellationContent(schedule)
                : null;

        List<Long> membersToNotify = !nextActive
                ? getRegisteredMemberIds(id)
                : List.of();

        // Neu bat lai lich hoc thi kiem tra trung PT/phong.
        if (nextActive) {
            ClassScheduleFormDto checkDto =
                    ClassScheduleFormDto.builder()
                            .id(schedule.getId())
                            .fitnessClassId(
                                    schedule.getFitnessClass().getId())
                            .scheduleDate(schedule.getScheduleDate())
                            .startTime(schedule.getStartTime())
                            .endTime(schedule.getEndTime())
                            .room(schedule.getRoom())
                            .active(true)
                            .build();

            validateScheduleBusinessRules(
                    checkDto,
                    schedule.getId()
            );
        }

        schedule.setActive(nextActive);
        scheduleRepository.save(schedule);

        // GD5-C: Chi gui thong bao khi DONG ca.
        if (!nextActive) {
            classRegistrationRepository.cancelRegistrationsForSchedule(
                    id, RegistrationStatus.REGISTERED, RegistrationStatus.CANCELLED);
            notifyScheduleCancellation(
                    id,
                    membersToNotify,
                    cancelledContent
            );
        }
    }

    // =====================================================
    // GD5-C: LAY DANH SACH MEMBER DANG KY BUOI HOC
    // =====================================================
    private List<Long> getRegisteredMemberIds(Long scheduleId) {
        return classRegistrationRepository
                .findDistinctUserIdsByScheduleAndStatus(
                        scheduleId,
                        RegistrationStatus.REGISTERED
                );
    }

    // =====================================================
    // GD5-C: TAO NOI DUNG THONG BAO HUY BUOI HOC
    // =====================================================
    private String cancellationContent(ClassSchedule schedule) {
        return "Buổi học '"
                + schedule.getFitnessClass().getName()
                + "' ngày "
                + schedule.getScheduleDate().format(DATE_FMT)
                + " ("
                + schedule.getStartTime()
                + " - "
                + schedule.getEndTime()
                + ") đã bị quản trị viên đóng/hủy. "
                + "Vui lòng kiểm tra lại lịch tập.";
    }

    // =====================================================
    // GD5-C: GUI THONG BAO QUA NOTIFICATIONSERVICE CUA B
    // =====================================================
    private void notifyScheduleCancellation(
            Long scheduleId,
            List<Long> memberIds,
            String content
    ) {
        for (Long memberId : memberIds) {
            notificationService.createNotificationForUser(
                    memberId,
                    NotificationType.CLASS_SCHEDULE_CANCELLED,
                    "Buổi học đã bị hủy",
                    content,
                    scheduleId
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<FitnessClass> getSchedulableClasses() {
        return fitnessClassRepository
                .findAllByStatusInOrderByNameAsc(
                        List.of(ClassStatus.OPEN, ClassStatus.FULL)
                );
    }

    @Override
    @Transactional(readOnly = true)
    public List<Branch> getAllBranches() {
        return branchRepository.findAll(
                Sort.by(Sort.Direction.ASC, "name")
        );
    }

    // =====================================================
    // BUSINESS RULES GIAI DOAN 4 - GIU NGUYEN
    // =====================================================
    private FitnessClass validateScheduleBusinessRules(
            ClassScheduleFormDto dto,
            Long currentScheduleId
    ) {
        // 1. Kiem tra thoi gian bat dau < ket thuc.
        if (dto.getStartTime() == null
                || dto.getEndTime() == null
                || !dto.getStartTime().isBefore(dto.getEndTime())) {

            throw new IllegalArgumentException(
                    "Giờ bắt đầu phải sớm hơn giờ kết thúc "
                            + "(startTime < endTime)!"
            );
        }

        if (dto.getScheduleDate() == null || !dto.getScheduleDate().atTime(dto.getStartTime()).isAfter(java.time.LocalDateTime.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh"))))
            throw new IllegalArgumentException("Chỉ xếp lịch trong tương lai.");
        // 2. Kiem tra trang thai FitnessClass.
        FitnessClass fitnessClass =
                fitnessClassRepository
                        .findDetailById(dto.getFitnessClassId())
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Không tìm thấy lớp tập đã chọn!"
                                ));

        if (fitnessClass.getStatus() == ClassStatus.CANCELLED
                || fitnessClass.getStatus() == ClassStatus.COMPLETED) {

            throw new IllegalArgumentException(
                    "Không thể xếp lịch hoặc kích hoạt lịch "
                            + "cho lớp tập đã HỦY hoặc ĐÃ KẾT THÚC ("
                            + fitnessClass.getName() + ")!"
            );
        }

        if (!Boolean.TRUE.equals(
                fitnessClass.getBranch().getActive())) {

            throw new IllegalArgumentException(
                    "Chi nhánh của lớp tập này hiện "
                            + "đang ngừng hoạt động!"
            );
        }

        if ((fitnessClass.getBranch().getOpeningTime() != null && dto.getStartTime().isBefore(fitnessClass.getBranch().getOpeningTime()))
                || (fitnessClass.getBranch().getClosingTime() != null && dto.getEndTime().isAfter(fitnessClass.getBranch().getClosingTime())))
            throw new IllegalArgumentException("Lịch phải nằm trong giờ mở cửa chi nhánh.");
        boolean isActiveSchedule =
                dto.getActive() == null
                        || Boolean.TRUE.equals(dto.getActive());

        Long safeExcludeId =
                currentScheduleId != null
                        ? currentScheduleId
                        : -1L;

        // 3. Kiem tra trung gio PT.
        Trainer trainer = fitnessClass.getTrainer();

        if (isActiveSchedule && trainer != null) {
            // Đồng bộ đặt lịch nhóm và lịch 1:1 theo cùng User PT.
            userRepository.lockByIdForBooking(trainer.getUser().getId())
                    .orElseThrow(() -> new IllegalStateException("PT không tồn tại."));
            // Kiểm tra chéo lịch nhóm và lịch PT 1:1.
            conflicts.assertTrainerFree(trainer.getId(), dto.getScheduleDate(),
                    dto.getStartTime(), dto.getEndTime(), safeExcludeId);
            if (!Boolean.TRUE.equals(trainer.getActive())) {
                throw new IllegalArgumentException(
                        "Huấn luyện viên phụ trách lớp này "
                                + "hiện đang ngừng hoạt động!"
                );
            }

            List<ClassSchedule> trainerConflicts =
                    scheduleRepository.findOverlappingTrainerSchedules(
                            trainer.getId(),
                            dto.getScheduleDate(),
                            dto.getStartTime(),
                            dto.getEndTime(),
                            ClassStatus.CANCELLED,
                            safeExcludeId
                    );

            if (!trainerConflicts.isEmpty()) {
                ClassSchedule conflict = trainerConflicts.get(0);

                String ptName =
                        trainer.getUser().getFirstName()
                                + " "
                                + trainer.getUser().getLastName();

                throw new IllegalArgumentException(
                        String.format(
                                "Trùng lịch PT! Huấn luyện viên %s "
                                        + "đã có lịch dạy lớp '%s' "
                                        + "từ %s đến %s ngày %s.",
                                ptName,
                                conflict.getFitnessClass().getName(),
                                conflict.getStartTime(),
                                conflict.getEndTime(),
                                conflict.getScheduleDate().format(DATE_FMT)
                        )
                );
            }
        }

        // 4. Kiem tra trung phong tap.
        if (isActiveSchedule
                && dto.getRoom() != null
                && !dto.getRoom().trim().isEmpty()) {

            List<ClassSchedule> roomConflicts =
                    scheduleRepository.findOverlappingRoomSchedules(
                            fitnessClass.getBranch().getId(),
                            dto.getScheduleDate(),
                            dto.getStartTime(),
                            dto.getEndTime(),
                            dto.getRoom().trim(),
                            ClassStatus.CANCELLED,
                            safeExcludeId
                    );

            if (!roomConflicts.isEmpty()) {
                ClassSchedule conflict = roomConflicts.get(0);

                throw new IllegalArgumentException(
                        String.format(
                                "Trùng phòng tập! Phòng '%s' "
                                        + "tại chi nhánh %s "
                                        + "đã được xếp cho lớp '%s' "
                                        + "(%s - %s) ngày %s.",
                                dto.getRoom().trim(),
                                fitnessClass.getBranch().getName(),
                                conflict.getFitnessClass().getName(),
                                conflict.getStartTime(),
                                conflict.getEndTime(),
                                conflict.getScheduleDate().format(DATE_FMT)
                        )
                );
            }
        }

        return fitnessClass;
    }
}
