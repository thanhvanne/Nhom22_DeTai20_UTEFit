package com.utefit.service.impl;

import com.utefit.dto.admin.ClassScheduleFormDto;
import com.utefit.entity.Branch;
import com.utefit.entity.ClassSchedule;
import com.utefit.entity.FitnessClass;
import com.utefit.entity.Trainer;
import com.utefit.enums.ClassStatus;
import com.utefit.repository.BranchRepository;
import com.utefit.repository.ClassScheduleRepository;
import com.utefit.repository.FitnessClassRepository;
import com.utefit.service.AdminScheduleService;
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
public class AdminScheduleServiceImpl implements AdminScheduleService {

    private final ClassScheduleRepository scheduleRepository;
    private final FitnessClassRepository fitnessClassRepository;
    private final BranchRepository branchRepository;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

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
                page,
                size,
                Sort.by(Sort.Direction.ASC, "scheduleDate")
                        .and(Sort.by(Sort.Direction.ASC, "startTime"))
        );
        return scheduleRepository.searchAdminSchedules(fitnessClassId, branchId, scheduleDate, active, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public ClassSchedule getScheduleById(Long id) {
        return scheduleRepository.findDetailById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lịch lớp tập với ID: " + id));
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
        FitnessClass fitnessClass = validateScheduleBusinessRules(dto, null);

        ClassSchedule schedule = ClassSchedule.builder()
                .fitnessClass(fitnessClass)
                .scheduleDate(dto.getScheduleDate())
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .room(dto.getRoom() != null && !dto.getRoom().trim().isEmpty() ? dto.getRoom().trim() : null)
                .active(dto.getActive() != null ? dto.getActive() : true)
                .build();

        scheduleRepository.save(schedule);
    }

    @Override
    @Transactional
    public void updateSchedule(Long id, ClassScheduleFormDto dto) {
        ClassSchedule schedule = getScheduleById(id);
        FitnessClass fitnessClass = validateScheduleBusinessRules(dto, id);

        schedule.setFitnessClass(fitnessClass);
        schedule.setScheduleDate(dto.getScheduleDate());
        schedule.setStartTime(dto.getStartTime());
        schedule.setEndTime(dto.getEndTime());
        schedule.setRoom(dto.getRoom() != null && !dto.getRoom().trim().isEmpty() ? dto.getRoom().trim() : null);
        schedule.setActive(dto.getActive() != null ? dto.getActive() : true);

        scheduleRepository.save(schedule);
    }

    @Override
    @Transactional
    public void toggleActive(Long id) {
        ClassSchedule schedule = getScheduleById(id);
        boolean nextActive = !Boolean.TRUE.equals(schedule.getActive());

        // Nếu bật lại lịch học, kiểm tra lại điều kiện chống trùng lịch PT & phòng
        if (nextActive) {
            ClassScheduleFormDto checkDto = ClassScheduleFormDto.builder()
                    .id(schedule.getId())
                    .fitnessClassId(schedule.getFitnessClass().getId())
                    .scheduleDate(schedule.getScheduleDate())
                    .startTime(schedule.getStartTime())
                    .endTime(schedule.getEndTime())
                    .room(schedule.getRoom())
                    .active(true)
                    .build();
            validateScheduleBusinessRules(checkDto, schedule.getId());
        }

        schedule.setActive(nextActive);
        scheduleRepository.save(schedule);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FitnessClass> getSchedulableClasses() {
        return fitnessClassRepository.findAllByStatusInOrderByNameAsc(
                List.of(ClassStatus.OPEN, ClassStatus.FULL)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<Branch> getAllBranches() {
        return branchRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
    }

    // ==================== BUSINESS RULES TRỌNG TÂM GIAI ĐOẠN 4 ====================
    private FitnessClass validateScheduleBusinessRules(ClassScheduleFormDto dto, Long currentScheduleId) {
        // 1. Kiểm tra startTime < endTime
        if (dto.getStartTime() == null || dto.getEndTime() == null || !dto.getStartTime().isBefore(dto.getEndTime())) {
            throw new IllegalArgumentException("Giờ bắt đầu phải sớm hơn giờ kết thúc (startTime < endTime)!");
        }

        // 2. Kiểm tra lớp tập hợp lệ và không bị CANCELLED / COMPLETED
        FitnessClass fitnessClass = fitnessClassRepository.findDetailById(dto.getFitnessClassId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lớp tập đã chọn!"));

        if (fitnessClass.getStatus() == ClassStatus.CANCELLED || fitnessClass.getStatus() == ClassStatus.COMPLETED) {
            throw new IllegalArgumentException(
                    "Không thể xếp lịch hoặc kích hoạt lịch cho lớp tập đã HỦY hoặc ĐÃ KẾT THÚC (" + fitnessClass.getName() + ")!"
            );
        }

        if (!Boolean.TRUE.equals(fitnessClass.getBranch().getActive())) {
            throw new IllegalArgumentException("Chi nhánh của lớp tập này hiện đang ngừng hoạt động!");
        }

        boolean isActiveSchedule = dto.getActive() == null || Boolean.TRUE.equals(dto.getActive());
        Long safeExcludeId = (currentScheduleId != null) ? currentScheduleId : -1L;

        // 3. Kiểm tra chống trùng lịch PT (Trainer Time Overlap) nếu lớp có gán PT và lịch đang active
        Trainer trainer = fitnessClass.getTrainer();
        if (isActiveSchedule && trainer != null) {
            if (!Boolean.TRUE.equals(trainer.getActive())) {
                throw new IllegalArgumentException("Huấn luyện viên phụ trách lớp này hiện đang ngừng hoạt động!");
            }

            List<ClassSchedule> trainerConflicts = scheduleRepository.findOverlappingTrainerSchedules(
                    trainer.getId(),
                    dto.getScheduleDate(),
                    dto.getStartTime(),
                    dto.getEndTime(),
                    ClassStatus.CANCELLED,
                    safeExcludeId
            );

            if (!trainerConflicts.isEmpty()) {
                ClassSchedule conflict = trainerConflicts.get(0);
                String ptName = trainer.getUser().getFirstName() + " " + trainer.getUser().getLastName();
                throw new IllegalArgumentException(
                        String.format(
                                "Trùng lịch PT! Huấn luyện viên %s đã có lịch dạy lớp '%s' từ %s đến %s ngày %s.",
                                ptName,
                                conflict.getFitnessClass().getName(),
                                conflict.getStartTime(),
                                conflict.getEndTime(),
                                conflict.getScheduleDate().format(DATE_FMT)
                        )
                );
            }
        }

        // 4. Kiểm tra chống trùng phòng tập (Room Overlap) trong cùng chi nhánh
        if (isActiveSchedule && dto.getRoom() != null && !dto.getRoom().trim().isEmpty()) {
            List<ClassSchedule> roomConflicts = scheduleRepository.findOverlappingRoomSchedules(
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
                                "Trùng phòng tập! Phòng '%s' tại chi nhánh %s đã được xếp cho lớp '%s' (%s - %s) ngày %s.",
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