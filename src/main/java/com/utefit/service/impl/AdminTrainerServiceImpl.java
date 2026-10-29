package com.utefit.service.impl;

import com.utefit.entity.Branch;
import com.utefit.entity.Trainer;
import com.utefit.enums.UserRole;
import com.utefit.enums.UserStatus;
import com.utefit.repository.BranchRepository;
import com.utefit.repository.TrainerRepository;
import com.utefit.repository.PersonalTrainingScheduleRepository;
import com.utefit.repository.ClassScheduleRepository;
import com.utefit.enums.ClassStatus;
import java.time.LocalDate;
import java.util.List;
import com.utefit.service.AdminTrainerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminTrainerServiceImpl implements AdminTrainerService {

    private final TrainerRepository trainerRepository;
    private final BranchRepository branchRepository;
    private final PersonalTrainingScheduleRepository personalSchedules;
    private final ClassScheduleRepository groupSchedules;

    @Override
    @Transactional(readOnly = true)
    public Page<Trainer> getTrainers(String keyword, Long branchId, Boolean active, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, Math.min(100, size)), Sort.by(Sort.Direction.ASC, "id"));
        String searchPattern = (keyword != null && !keyword.trim().isEmpty())
                ? "%" + keyword.trim().toLowerCase() + "%"
                : null;
        return trainerRepository.searchAdminTrainers(searchPattern, branchId, active, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Trainer getTrainerById(Long id) {
        return trainerRepository.findAdminTrainerById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy huấn luyện viên với ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Branch> getAllBranches() {
        return branchRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
    }

    @Override
    @Transactional
    public void assignBranch(Long trainerId, Long branchId) {
        Trainer trainer = getTrainerById(trainerId);

        boolean changingBranch = (trainer.getBranch() == null && branchId != null)
                || (trainer.getBranch() != null && !trainer.getBranch().getId().equals(branchId));
        if (changingBranch) requireNoUpcomingSchedules(trainer);
        if (branchId == null) {
            trainer.setBranch(null);
        } else {
            Branch branch = branchRepository.findById(branchId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chi nhánh với ID: " + branchId));
            if (!Boolean.TRUE.equals(branch.getActive())) {
                throw new IllegalArgumentException("Không thể gán PT vào chi nhánh ngừng hoạt động.");
            }
            trainer.setBranch(branch);
        }

        trainerRepository.save(trainer);
    }

    private void requireNoUpcomingSchedules(Trainer trainer) {
        LocalDate today = LocalDate.now();
        if (personalSchedules.findByTrainer_IdAndScheduleDateAndActiveTrue(trainer.getId(), today)
                .stream().anyMatch(s -> !s.getEndTime().isBefore(java.time.LocalTime.now()))
                || personalSchedules.existsByTrainer_IdAndActiveTrueAndScheduleDateAfter(trainer.getId(), today)
                || groupSchedules.findActiveTrainerSessions(trainer.getId(), today,
                        List.of(ClassStatus.CANCELLED, ClassStatus.COMPLETED)).stream()
                .anyMatch(s -> !s.getEndTime().isBefore(java.time.LocalTime.now()))
                || groupSchedules.existsUpcomingTrainerSessions(trainer.getId(), today,
                ClassStatus.CANCELLED, ClassStatus.COMPLETED)) {
            throw new IllegalStateException("PT còn lịch trong tương lai; hủy hoặc xử lý lịch trước khi chuyển/tắt PT.");
        }
    }

    @Override
    @Transactional
    public void toggleActive(Long trainerId) {
        Trainer trainer = getTrainerById(trainerId);
        boolean next = !Boolean.TRUE.equals(trainer.getActive());
        if (next && (trainer.getUser().getRole() != UserRole.PT
                || trainer.getUser().getStatus() != UserStatus.ACTIVE
                || !Boolean.TRUE.equals(trainer.getUser().getEnabled()))) {
            throw new IllegalStateException("Chỉ kích hoạt PT khi tài khoản hợp lệ và ACTIVE.");
        }
        if (next && trainer.getBranch() == null) throw new IllegalStateException("Cần gán chi nhánh trước khi kích hoạt PT.");
        if (next && trainer.getBranch() != null
                && !Boolean.TRUE.equals(trainer.getBranch().getActive())) {
            throw new IllegalStateException("Không kích hoạt PT ở chi nhánh ngừng hoạt động.");
        }
        if (!next) requireNoUpcomingSchedules(trainer);
        trainer.setActive(next);
        trainerRepository.save(trainer);
    }
}
